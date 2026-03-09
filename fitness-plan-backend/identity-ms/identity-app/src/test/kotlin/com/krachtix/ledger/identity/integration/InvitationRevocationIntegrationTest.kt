package com.krachtix.identity.integration

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.kyc.TrustLevel
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.config.BaseIntegrationTest
import com.krachtix.identity.core.entity.OAuthClientSettingName
import com.krachtix.identity.core.entity.OAuthRegisteredClient
import com.krachtix.identity.core.entity.OAuthTokenSettingName
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.RegistrationSource
import com.krachtix.identity.core.entity.UserType
import com.krachtix.identity.core.invitation.command.RevokeInvitationCommand
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.OAuthClientConfigService
import an.awesome.pipelinr.Pipeline
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Transactional
class InvitationRevocationIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var pipeline: Pipeline

    @Autowired
    private lateinit var userRepository: OAuthUserRepository

    @Autowired
    private lateinit var oauthClientRepository: OAuthRegisteredClientRepository

    @Autowired
    private lateinit var oauthClientConfigService: OAuthClientConfigService

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var processGateway: ProcessGateway

    private lateinit var oauthClient: OAuthRegisteredClient
    private lateinit var adminUser: OAuthUser
    private lateinit var pendingUser: OAuthUser

    @BeforeEach
    fun setUpTestData() {
        oauthClient = OAuthRegisteredClient().apply {
            id = UUID.randomUUID()
            clientId = UUID.randomUUID().toString()
            clientName = "Test Company"
            clientIdIssuedAt = Instant.now()
            sandboxClientSecret = passwordEncoder.encode("sandbox-secret")
            productionClientSecret = passwordEncoder.encode("prod-secret")
            domain = "test-${UUID.randomUUID().toString().take(8)}.com"
            failedAuthAttempts = 0

            addAuthenticationMethod(oauthClientConfigService.getAuthenticationMethod("client_secret_basic"))
            addAuthenticationMethod(oauthClientConfigService.getAuthenticationMethod("client_secret_post"))
            addGrantType(oauthClientConfigService.getGrantType("client_credentials"))
            addGrantType(oauthClientConfigService.getGrantType("refresh_token"))
            addScope(oauthClientConfigService.getScope("openid"))
            addScope(oauthClientConfigService.getScope("profile"))
            addScope(oauthClientConfigService.getScope("email"))
            addSetting(OAuthClientSettingName.REQUIRE_AUTHORIZATION_CONSENT, "false")
            addSetting(OAuthClientSettingName.REQUIRE_PROOF_KEY, "false")
            addTokenSetting(OAuthTokenSettingName.ACCESS_TOKEN_TIME_TO_LIVE, "PT30M")
            addTokenSetting(OAuthTokenSettingName.REFRESH_TOKEN_TIME_TO_LIVE, "PT12H")
            addTokenSetting(OAuthTokenSettingName.REUSE_REFRESH_TOKENS, "false")
        }
        oauthClientRepository.save(oauthClient)

        adminUser = OAuthUser(
            username = "admin@${oauthClient.domain}",
            password = passwordEncoder.encode("AdminPassword123!")!!,
            email = Email("admin@${oauthClient.domain}")
        ).apply {
            firstName = "Admin"
            lastName = "User"
            userType = UserType.BUSINESS
            trustLevel = TrustLevel.TIER_THREE
            registrationSource = RegistrationSource.SELF_REGISTRATION
            enabled = true
            emailVerified = true
            invitationStatus = true
            merchantId = oauthClient.id
            authorities = mutableSetOf("ROLE_MERCHANT_ADMIN", "ROLE_MERCHANT_SUPER_ADMIN", "ROLE_MERCHANT_USER")
        }
        userRepository.save(adminUser)

        pendingUser = OAuthUser(
            username = "pending@${oauthClient.domain}",
            password = passwordEncoder.encode("TempPassword123!")!!,
            email = Email("pending@${oauthClient.domain}")
        ).apply {
            userType = UserType.BUSINESS
            trustLevel = TrustLevel.TIER_THREE
            enabled = true
            emailVerified = false
            invitationStatus = false
            merchantId = oauthClient.id
            authorities = mutableSetOf("ROLE_MERCHANT_USER")
        }
        userRepository.save(pendingUser)
    }

    @Nested
    inner class SuccessfulRevocation {

        @Test
        fun `should revoke pending invitation and anonymize user`() {
            processGateway.createProcess(
                CreateNewProcessPayload(
                    userId = pendingUser.id!!,
                    publicId = UUID.randomUUID(),
                    type = ProcessType.MERCHANT_USER_INVITATION,
                    description = "Invitation for pending user",
                    initialState = ProcessState.PENDING,
                    requestState = ProcessState.COMPLETE,
                    channel = ProcessChannel.SYSTEM,
                    data = mapOf(
                        ProcessRequestDataName.USER_IDENTIFIER to pendingUser.id.toString(),
                        ProcessRequestDataName.MERCHANT_ID to oauthClient.id.toString()
                    ),
                    stakeholders = mapOf(
                        ProcessStakeholderType.FOR_USER to pendingUser.id.toString()
                    )
                )
            )

            val result = pipeline.send(
                RevokeInvitationCommand(
                    targetUserId = pendingUser.id!!,
                    revokedByUserId = adminUser.id!!
                )
            )

            assertThat(result.success).isTrue()
            assertThat(result.message).isNotBlank()
        }

        @Test
        fun `should anonymize revoked user so they are not findable by original email`() {
            val originalEmail = pendingUser.email?.value!!

            pipeline.send(
                RevokeInvitationCommand(
                    targetUserId = pendingUser.id!!,
                    revokedByUserId = adminUser.id!!
                )
            )

            val foundUser = userRepository.findByEmail(Email(originalEmail))
            assertThat(foundUser).isNull()
        }

        @Test
        fun `should null merchantId and email on anonymized user`() {
            val pendingUserId = pendingUser.id!!

            pipeline.send(
                RevokeInvitationCommand(
                    targetUserId = pendingUserId,
                    revokedByUserId = adminUser.id!!
                )
            )

            val anonymizedUser = userRepository.findById(pendingUserId).orElse(null)
            assertThat(anonymizedUser).isNotNull()
            assertThat(anonymizedUser!!.merchantId).isNull()
            assertThat(anonymizedUser.organizationId).isNull()
            assertThat(anonymizedUser.email).isNull()
            assertThat(anonymizedUser.enabled).isFalse()
        }

        @Test
        fun `should not include anonymized user in merchant team list`() {
            val pendingUserId = pendingUser.id!!

            pipeline.send(
                RevokeInvitationCommand(
                    targetUserId = pendingUserId,
                    revokedByUserId = adminUser.id!!
                )
            )

            val merchantUsers = userRepository.findByMerchantId(oauthClient.id!!)
            assertThat(merchantUsers).noneMatch { it.id == pendingUserId }
        }

        @Test
        fun `should fail the pending invitation process`() {
            val invitationProcess = processGateway.createProcess(
                CreateNewProcessPayload(
                    userId = pendingUser.id!!,
                    publicId = UUID.randomUUID(),
                    type = ProcessType.MERCHANT_USER_INVITATION,
                    description = "Invitation for pending user",
                    initialState = ProcessState.PENDING,
                    requestState = ProcessState.COMPLETE,
                    channel = ProcessChannel.SYSTEM,
                    data = mapOf(
                        ProcessRequestDataName.USER_IDENTIFIER to pendingUser.id.toString(),
                        ProcessRequestDataName.MERCHANT_ID to oauthClient.id.toString()
                    ),
                    stakeholders = mapOf(
                        ProcessStakeholderType.FOR_USER to pendingUser.id.toString()
                    )
                )
            )

            pipeline.send(
                RevokeInvitationCommand(
                    targetUserId = pendingUser.id!!,
                    revokedByUserId = adminUser.id!!
                )
            )

            val failedProcess = processGateway.findLatestPendingProcessesByTypeAndForUserId(
                ProcessType.MERCHANT_USER_INVITATION,
                pendingUser.id!!
            )
            assertThat(failedProcess).isNull()
        }
    }

    @Nested
    inner class ValidationErrors {

        @Test
        fun `should reject revoking an active user`() {
            val activeUser = OAuthUser(
                username = "active@${oauthClient.domain}",
                password = passwordEncoder.encode("ActivePassword123!")!!,
                email = Email("active@${oauthClient.domain}")
            ).apply {
                userType = UserType.BUSINESS
                trustLevel = TrustLevel.TIER_THREE
                enabled = true
                emailVerified = true
                invitationStatus = true
                merchantId = oauthClient.id
                authorities = mutableSetOf("ROLE_MERCHANT_USER")
            }
            userRepository.save(activeUser)

            assertThatThrownBy {
                pipeline.send(
                    RevokeInvitationCommand(
                        targetUserId = activeUser.id!!,
                        revokedByUserId = adminUser.id!!
                    )
                )
            }.isInstanceOf(InvalidRequestException::class.java)
        }

        @Test
        fun `should reject revoking self`() {
            assertThatThrownBy {
                pipeline.send(
                    RevokeInvitationCommand(
                        targetUserId = adminUser.id!!,
                        revokedByUserId = adminUser.id!!
                    )
                )
            }.isInstanceOf(InvalidRequestException::class.java)
        }

        @Test
        fun `should reject revoking user from different merchant`() {
            val otherClient = OAuthRegisteredClient().apply {
                id = UUID.randomUUID()
                clientId = UUID.randomUUID().toString()
                clientName = "Other Company"
                clientIdIssuedAt = Instant.now()
                sandboxClientSecret = passwordEncoder.encode("sandbox-secret")
                productionClientSecret = passwordEncoder.encode("prod-secret")
                domain = "other-${UUID.randomUUID().toString().take(8)}.com"
                failedAuthAttempts = 0

                addAuthenticationMethod(oauthClientConfigService.getAuthenticationMethod("client_secret_basic"))
                addAuthenticationMethod(oauthClientConfigService.getAuthenticationMethod("client_secret_post"))
                addGrantType(oauthClientConfigService.getGrantType("client_credentials"))
                addGrantType(oauthClientConfigService.getGrantType("refresh_token"))
                addScope(oauthClientConfigService.getScope("openid"))
                addScope(oauthClientConfigService.getScope("profile"))
                addScope(oauthClientConfigService.getScope("email"))
                addSetting(OAuthClientSettingName.REQUIRE_AUTHORIZATION_CONSENT, "false")
                addSetting(OAuthClientSettingName.REQUIRE_PROOF_KEY, "false")
                addTokenSetting(OAuthTokenSettingName.ACCESS_TOKEN_TIME_TO_LIVE, "PT30M")
                addTokenSetting(OAuthTokenSettingName.REFRESH_TOKEN_TIME_TO_LIVE, "PT12H")
                addTokenSetting(OAuthTokenSettingName.REUSE_REFRESH_TOKENS, "false")
            }
            oauthClientRepository.save(otherClient)

            val otherMerchantUser = OAuthUser(
                username = "other@${otherClient.domain}",
                password = passwordEncoder.encode("OtherPassword123!")!!,
                email = Email("other@${otherClient.domain}")
            ).apply {
                userType = UserType.BUSINESS
                trustLevel = TrustLevel.TIER_THREE
                enabled = true
                emailVerified = false
                invitationStatus = false
                merchantId = otherClient.id
                authorities = mutableSetOf("ROLE_MERCHANT_USER")
            }
            userRepository.save(otherMerchantUser)

            assertThatThrownBy {
                pipeline.send(
                    RevokeInvitationCommand(
                        targetUserId = otherMerchantUser.id!!,
                        revokedByUserId = adminUser.id!!
                    )
                )
            }.isInstanceOf(RecordNotFoundException::class.java)
        }
    }
}
