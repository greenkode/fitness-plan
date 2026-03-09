package com.krachtix.identity.core.registration.command

import com.krachtix.commons.notification.dto.MessageRecipient
import com.krachtix.commons.notification.enumeration.TemplateName
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.MakeProcessRequestPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.commons.dto.Email
import com.krachtix.identity.core.entity.OAuthClientSettingName
import com.krachtix.identity.core.entity.OAuthRegisteredClient
import com.krachtix.identity.core.entity.OAuthTokenSettingName
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.RegistrationSource
import com.krachtix.commons.kyc.TrustLevel
import com.krachtix.identity.core.entity.UserType
import com.krachtix.identity.core.integration.NotificationClient
import com.krachtix.identity.core.organization.entity.Organization
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.BusinessEmailValidationService
import com.krachtix.identity.core.service.OAuthClientConfigService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Locale
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class RegisterUserCommandHandler(
    private val userRepository: OAuthUserRepository,
    private val oauthClientRepository: OAuthRegisteredClientRepository,
    private val organizationRepository: OrganizationRepository,
    private val businessEmailValidationService: BusinessEmailValidationService,
    private val passwordEncoder: PasswordEncoder,
    private val processGateway: ProcessGateway,
    private val messageService: MessageService,
    private val oauthClientConfigService: OAuthClientConfigService,
    private val notificationClient: NotificationClient,
    @Value("\${app.registration.verification-base-url}")
    private val verificationBaseUrl: String
) : Command.Handler<RegisterUserCommand, RegisterUserResult> {

    override fun handle(command: RegisterUserCommand): RegisterUserResult {
        log.info { "Processing registration for email: ${command.email}" }

        val normalizedEmail = command.email.trim().lowercase()
        businessEmailValidationService.validateBusinessEmail(normalizedEmail)

        val existingUser = userRepository.findByEmail(Email(normalizedEmail))
        existingUser?.let { user ->
            if (user.registrationComplete) {
                throw InvalidRequestException(messageService.getMessage("registration.error.email_exists"))
            }
            log.info { "Found incomplete registration for email: $normalizedEmail, allowing re-registration" }
        }

        val domain = normalizedEmail.substringAfter("@")
        val existingClient = oauthClientRepository.findByDomain(domain)
        val isNewOrganization = existingClient == null && existingUser == null

        val (organizationId, oauthClientId) = existingUser?.organizationId?.let { orgId ->
            orgId to existingUser.merchantId
        } ?: existingClient?.id?.let { clientId ->
            val org = organizationRepository.findBySlug(generateSlug(domain))
            (org?.id ?: clientId) to clientId
        } ?: run {
            val (org, client) = createOrganizationAndOAuthClient(domain, command.organizationName, normalizedEmail)
            org.id to client.id
        }

        val user = existingUser?.let { updateExistingUser(it, command, organizationId, oauthClientId) }
            ?: createUser(command, normalizedEmail, organizationId, oauthClientId, isNewOrganization)

        cancelExistingVerificationProcess(user)
        createEmailVerificationProcess(user, normalizedEmail)

        if (isNewOrganization && organizationId != null && oauthClientId != null) {
            createOrganizationSetupProcess(user, oauthClientId, organizationId)
        }

        log.info { "User registered successfully: ${user.id}, organization: $organizationId, isNewOrg: $isNewOrganization" }

        return RegisterUserResult(
            success = true,
            message = messageService.getMessage("registration.success.user_created"),
            userId = user.id,
            organizationId = organizationId,
            isNewOrganization = isNewOrganization,
            verificationRequired = true
        )
    }

    private fun generateSlug(domain: String): String {
        return domain.substringBefore(".")
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }

    private fun createOrganizationAndOAuthClient(
        domain: String,
        organizationName: String,
        adminEmail: String
    ): Pair<Organization, OAuthRegisteredClient> {
        if (organizationRepository.existsByNameIgnoreCase(organizationName)) {
            throw InvalidRequestException(messageService.getMessage("registration.error.organization_name_exists"))
        }

        val slug = generateUniqueSlug(generateSlug(domain))

        val organization = Organization(
            name = organizationName,
            slug = slug
        )
        organizationRepository.save(organization)
        log.info { "Created Organization: ${organization.id} with name: $organizationName, slug: $slug" }

        val oauthClient = createOAuthClientForOrganization(organization, domain)

        organization.addProperty(OrganizationPropertyName.EMAIL, adminEmail)
        organizationRepository.save(organization)

        return organization to oauthClient
    }

    private fun generateUniqueSlug(baseSlug: String): String {
        var slug = baseSlug
        var counter = 1
        while (organizationRepository.existsBySlug(slug)) {
            slug = "$baseSlug-$counter"
            counter++
        }
        return slug
    }

    private fun createOAuthClientForOrganization(
        organization: Organization,
        domain: String
    ): OAuthRegisteredClient {
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

        val oauthClient = OAuthRegisteredClient().apply {
            id = organization.id!!
            clientId = UUID.randomUUID().toString()
            clientName = organization.name
            clientIdIssuedAt = Instant.now()
            sandboxClientSecret = passwordEncoder.encode(sandboxSecret)
            productionClientSecret = passwordEncoder.encode(productionSecret)
            this.domain = domain
            this.organizationId = organization.id
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
            addSetting(OAuthClientSettingName.REQUIRE_AUTHORIZATION_CONSENT, "false")
            addSetting(OAuthClientSettingName.REQUIRE_PROOF_KEY, "false")
            addTokenSetting(OAuthTokenSettingName.ACCESS_TOKEN_TIME_TO_LIVE, "PT30M")
            addTokenSetting(OAuthTokenSettingName.REFRESH_TOKEN_TIME_TO_LIVE, "PT12H")
            addTokenSetting(OAuthTokenSettingName.REUSE_REFRESH_TOKENS, "false")
        }

        oauthClientRepository.save(oauthClient)
        log.info { "Created OAuth client for organization: ${organization.name} with clientId: ${oauthClient.clientId}, domain: $domain" }

        return oauthClient
    }

    private fun createUser(
        command: RegisterUserCommand,
        normalizedEmail: String,
        organizationId: UUID,
        oauthClientId: UUID?,
        isFirstUser: Boolean
    ): OAuthUser {
        val nameParts = command.fullName.trim().split("\\s+".toRegex(), 2)
        val firstName = nameParts[0]
        val lastName = nameParts.getOrNull(1) ?: ""

        val authorities = mutableSetOf("ROLE_USER").apply {
            if (isFirstUser) {
                add("ROLE_ADMIN")
                add("ROLE_SUPER_ADMIN")
                add("ROLE_MERCHANT_ADMIN")
                add("ROLE_MERCHANT_SUPER_ADMIN")
                add("ROLE_MERCHANT_USER")
            }
        }

        val user = OAuthUser().apply {
            this.username = normalizedEmail
            this.email = Email(normalizedEmail)
            this.password = passwordEncoder.encode(command.password)!!
            this.firstName = firstName
            this.lastName = lastName
            this.organizationId = organizationId
            this.merchantId = oauthClientId
            this.userType = UserType.BUSINESS
            this.trustLevel = TrustLevel.TIER_THREE
            this.registrationSource = RegistrationSource.SELF_REGISTRATION
            this.emailVerified = false
            this.enabled = true
            this.authorities = authorities
        }

        return userRepository.save(user)
    }

    private fun updateExistingUser(
        existingUser: OAuthUser,
        command: RegisterUserCommand,
        organizationId: UUID,
        oauthClientId: UUID?
    ): OAuthUser {
        val nameParts = command.fullName.trim().split("\\s+".toRegex(), 2)
        val firstName = nameParts[0]
        val lastName = nameParts.getOrNull(1) ?: ""

        existingUser.apply {
            this.password = passwordEncoder.encode(command.password)!!
            this.firstName = firstName
            this.lastName = lastName
            this.organizationId = organizationId
            this.merchantId = oauthClientId
            this.emailVerified = false
            this.registrationComplete = false
        }

        log.info { "Updated existing user: ${existingUser.id} with new registration details" }
        return userRepository.save(existingUser)
    }

    private fun cancelExistingVerificationProcess(user: OAuthUser) {
        processGateway.findLatestPendingProcessesByTypeAndForUserId(
            processType = ProcessType.EMAIL_VERIFICATION,
            userId = user.id!!
        )?.let { existingProcess ->
            val cancelRequest = MakeProcessRequestPayload(
                userId = user.id!!,
                publicId = existingProcess.publicId,
                eventType = ProcessEvent.PROCESS_FAILED,
                requestType = ProcessRequestType.FAIL_PROCESS,
                channel = ProcessChannel.BUSINESS_WEB
            )
            processGateway.makeRequest(cancelRequest)
            log.info { "Cancelled existing verification process for user: ${user.id}" }
        }
    }

    private fun createEmailVerificationProcess(user: OAuthUser, email: String) {
        val verificationToken = RandomStringUtils.secure().nextAlphanumeric(100)
        val processPayload = CreateNewProcessPayload(
            userId = user.id!!,
            publicId = UUID.randomUUID(),
            type = ProcessType.EMAIL_VERIFICATION,
            description = "Email verification for ${user.email?.value}",
            initialState = ProcessState.PENDING,
            requestState = ProcessState.PENDING,
            channel = ProcessChannel.BUSINESS_WEB,
            data = mapOf(
                ProcessRequestDataName.USER_EMAIL to email,
                ProcessRequestDataName.USER_IDENTIFIER to user.id.toString(),
                ProcessRequestDataName.VERIFICATION_TOKEN to verificationToken
            ),
            stakeholders = mapOf(
                ProcessStakeholderType.FOR_USER to user.id.toString()
            ),
            externalReference = verificationToken
        )

        processGateway.createProcess(processPayload)
        log.info { "Email verification process created for user: ${user.id}" }

        sendVerificationEmail(user, verificationToken)
    }

    private fun sendVerificationEmail(user: OAuthUser, token: String) {
        val verificationLink = "$verificationBaseUrl?token=$token"
        val expiryHours = ProcessType.EMAIL_VERIFICATION.timeInSeconds / 3600

        notificationClient.sendNotification(
            recipients = listOf(MessageRecipient(user.email?.value ?: "", user.firstName)),
            templateName = TemplateName.EMAIL_VERIFICATION,
            parameters = mapOf(
                "name" to (user.firstName ?: user.email?.value ?: ""),
                "verification_link" to verificationLink,
                "expiry_hours" to "$expiryHours"
            ),
            locale = Locale.ENGLISH,
            clientIdentifier = UUID.randomUUID().toString()
        )

        log.info { "Verification email sent to: ${user.email?.value}" }
    }

    private fun createOrganizationSetupProcess(user: OAuthUser, clientId: UUID, organizationId: UUID) {
        processGateway.createProcess(
            CreateNewProcessPayload(
                userId = user.id!!,
                publicId = UUID.randomUUID(),
                type = ProcessType.ORGANIZATION_SETUP,
                description = "Organization setup for ${user.email?.value}",
                initialState = ProcessState.PENDING,
                requestState = ProcessState.PENDING,
                channel = ProcessChannel.WEB_APP,
                data = mapOf(
                    ProcessRequestDataName.MERCHANT_ID to clientId.toString(),
                    ProcessRequestDataName.ORGANIZATION_ID to organizationId.toString(),
                    ProcessRequestDataName.USER_IDENTIFIER to user.id.toString()
                ),
                stakeholders = mapOf(
                    ProcessStakeholderType.FOR_USER to user.id.toString()
                )
            )
        )
        log.info { "Organization setup process created for user: ${user.id}" }
    }
}
