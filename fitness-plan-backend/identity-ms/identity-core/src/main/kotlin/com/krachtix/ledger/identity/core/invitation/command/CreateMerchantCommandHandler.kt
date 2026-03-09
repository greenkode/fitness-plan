package com.krachtix.identity.core.invitation.command

import com.krachtix.commons.dto.Email
import com.krachtix.identity.commons.audit.AuditEvent
import com.krachtix.identity.commons.audit.AuditResource
import com.krachtix.identity.commons.audit.IdentityType
import com.krachtix.commons.notification.dto.MessageRecipient
import com.krachtix.commons.notification.enumeration.TemplateName
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.core.entity.OAuthClientSettingName
import com.krachtix.identity.core.entity.OAuthRegisteredClient
import com.krachtix.identity.core.entity.OAuthTokenSettingName
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.commons.kyc.TrustLevel
import com.krachtix.identity.core.entity.UserType
import com.krachtix.identity.core.invitation.dto.CreateMerchantResult
import com.krachtix.identity.core.integration.NotificationClient
import com.krachtix.identity.core.invitation.dto.CreateMerchantCommand
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.OAuthClientConfigService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class CreateMerchantCommandHandler(
    private val repository: OAuthRegisteredClientRepository,
    private val userRepository: OAuthUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val processGateway: ProcessGateway,
    private val notificationClient: NotificationClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val oauthClientConfigService: OAuthClientConfigService,
    @Value("\${app.merchant.invitation.base-url}")
    private val invitationBaseUrl: String
) : Command.Handler<CreateMerchantCommand, CreateMerchantResult> {

    override fun handle(command: CreateMerchantCommand): CreateMerchantResult {

        val convertedClientId = command.clientName.lowercase().replace(" ", "-")

        log.info { "Creating new merchant with clientId: $convertedClientId" }

        val newMerchantId = UUID.randomUUID()

        val adminUser = userRepository.findByUsername(command.adminEmail)
            ?: run {

                log.info { "Creating new admin user for email: ${command.adminEmail}" }

                userRepository.save(
                    OAuthUser(
                        username = command.adminEmail,
                        password = passwordEncoder.encode(RandomStringUtils.secure().nextAlphanumeric(12))!!,
                        email = Email(command.adminEmail),
                        enabled = true,
                        authorities = mutableSetOf("ROLE_MERCHANT_ADMIN", "ROLE_MERCHANT_SUPER_ADMIN", "ROLE_MERCHANT_FINANCE_ADMIN", "ROLE_MERCHANT_USER"),
                    ).apply {
                        userType = UserType.BUSINESS
                        trustLevel = TrustLevel.TIER_THREE
                        emailVerified = false
                        invitationStatus = true
                        merchantId = newMerchantId
                    }
                )
            }

        val existingClient = repository.findByClientId(convertedClientId)
            ?: run {

                val sandboxSecret = RandomStringUtils.secure().nextAlphanumeric(30)
                val productionSecret = RandomStringUtils.secure().nextAlphanumeric(30)

                val authMethodBasic = oauthClientConfigService.getAuthenticationMethod("client_secret_basic")
                val authMethodPost = oauthClientConfigService.getAuthenticationMethod("client_secret_post")
                val grantTypeCredentials = oauthClientConfigService.getGrantType("client_credentials")
                val grantTypeRefresh = oauthClientConfigService.getGrantType("refresh_token")
                val scopeOpenid = oauthClientConfigService.getScope("openid")
                val scopeProfile = oauthClientConfigService.getScope("profile")
                val scopeEmail = oauthClientConfigService.getScope("email")
                val scopeRead = oauthClientConfigService.getScope("read")
                val scopeWrite = oauthClientConfigService.getScope("write")
                val scopeMerchant = oauthClientConfigService.getScope("merchant")
                val scopeLedgerRead = oauthClientConfigService.getScope("krachtix:read")
                val scopeLedgerWrite = oauthClientConfigService.getScope("krachtix:write")
                val scopeLedgerAdmin = oauthClientConfigService.getScope("krachtix:admin")

                val client = OAuthRegisteredClient().apply {
                    id = newMerchantId
                    clientId = UUID.randomUUID().toString().uppercase().replace("-", "")
                    clientName = command.clientName
                    clientIdIssuedAt = Instant.now()
                    sandboxClientSecret = passwordEncoder.encode(sandboxSecret)
                    productionClientSecret = passwordEncoder.encode(productionSecret)
                    failedAuthAttempts = 0

                    addAuthenticationMethod(authMethodBasic)
                    addAuthenticationMethod(authMethodPost)
                    addGrantType(grantTypeCredentials)
                    addGrantType(grantTypeRefresh)
                    addScope(scopeOpenid)
                    addScope(scopeProfile)
                    addScope(scopeEmail)
                    addScope(scopeRead)
                    addScope(scopeWrite)
                    addScope(scopeMerchant)
                    addScope(scopeLedgerRead)
                    addScope(scopeLedgerWrite)
                    addScope(scopeLedgerAdmin)
                    addSetting(OAuthClientSettingName.REQUIRE_AUTHORIZATION_CONSENT, "false")
                    addSetting(OAuthClientSettingName.REQUIRE_PROOF_KEY, "false")
                    addTokenSetting(OAuthTokenSettingName.ACCESS_TOKEN_TIME_TO_LIVE, "PT30M")
                    addTokenSetting(OAuthTokenSettingName.REFRESH_TOKEN_TIME_TO_LIVE, "PT12H")
                    addTokenSetting(OAuthTokenSettingName.REUSE_REFRESH_TOKENS, "false")
                }

                repository.save(client)
            }

        log.info { "Creating merchant invitation process for ${adminUser.email?.value}" }

        val processId = UUID.randomUUID()
        val token = RandomStringUtils.secure().nextAlphanumeric(100)
        val authReference = UUID.randomUUID().toString()

        val process = processGateway.createProcess(
            CreateNewProcessPayload(
                userId = adminUser.id!!,
                publicId = processId,
                type = ProcessType.MERCHANT_USER_INVITATION,
                description = "Merchant invitation for ${adminUser.email?.value} to join ${existingClient.clientName}",
                initialState = ProcessState.PENDING,
                requestState = ProcessState.COMPLETE,
                channel = ProcessChannel.SYSTEM,
                externalReference = token,
                data = mapOf(
                    ProcessRequestDataName.USER_IDENTIFIER to adminUser.id.toString(),
                    ProcessRequestDataName.MERCHANT_ID to existingClient.id.toString(),
                    ProcessRequestDataName.AUTHENTICATION_REFERENCE to authReference
                ),
                stakeholders = mapOf(
                    ProcessStakeholderType.FOR_USER to adminUser.id.toString()
                )
            )
        )

        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

        notificationClient.sendNotification(
            recipients = listOf(MessageRecipient(address = adminUser.email?.value ?: "")),
            templateName = TemplateName.MERCHANT_USER_INVITATION,
            parameters = mapOf(
                "merchant_name" to existingClient.clientName,
                "invitation_url" to "$invitationBaseUrl?token=${process.externalReference}",
                "expiration_date" to formatter.format(
                    Instant.now().plusSeconds(process.type.timeInSeconds).atZone(ZoneId.systemDefault())
                )
            ),
            locale = Locale.ENGLISH,
            clientIdentifier = UUID.randomUUID().toString()
        )

        log.info { "Merchant invitation sent successfully to ${adminUser.email?.value}" }

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = adminUser.id.toString(),
                actorName = "${adminUser.firstName ?: ""} ${adminUser.lastName ?: ""}".trim().ifEmpty { adminUser.email?.value ?: "" },
                merchantId = newMerchantId.toString(),
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "Merchant created with invitation sent",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
            )
        )

        log.info { "Successfully created merchant with clientId: ${existingClient.clientId}" }

        return CreateMerchantResult(
            success = true,
            message = "Merchant created successfully",
            existingClient.id.toString()
        )
    }

}
