package com.krachtix.identity.integration

import com.krachtix.commons.dto.Email
import com.krachtix.commons.kyc.TrustLevel
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.MakeProcessRequestPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.config.BaseIntegrationTest
import com.krachtix.identity.core.entity.OAuthClientSettingName
import com.krachtix.identity.core.entity.OAuthRegisteredClient
import com.krachtix.identity.core.entity.OAuthTokenSettingName
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.OrganizationStatus
import com.krachtix.identity.core.entity.RegistrationSource
import com.krachtix.identity.core.entity.UserType
import com.krachtix.identity.core.organization.entity.Organization
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.OAuthClientConfigService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Transactional
class OrganizationSetupProcessIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var processGateway: ProcessGateway

    @Autowired
    private lateinit var userRepository: OAuthUserRepository

    @Autowired
    private lateinit var oauthClientRepository: OAuthRegisteredClientRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var oauthClientConfigService: OAuthClientConfigService

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var organization: Organization
    private lateinit var oauthClient: OAuthRegisteredClient
    private lateinit var user: OAuthUser

    @BeforeEach
    fun setUpTestData() {
        organization = Organization(
            name = "Test Corp",
            slug = "test-corp-${UUID.randomUUID().toString().take(8)}",
            status = OrganizationStatus.PENDING
        )
        organizationRepository.save(organization)

        oauthClient = OAuthRegisteredClient().apply {
            id = organization.id!!
            clientId = UUID.randomUUID().toString()
            clientName = "-"
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

        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "false")
        organizationRepository.save(organization)

        user = OAuthUser(
            username = "admin@${oauthClient.domain}",
            password = passwordEncoder.encode("TestPassword123!")!!,
            email = Email("admin@${oauthClient.domain}")
        ).apply {
            this.firstName = "Test"
            this.lastName = "Admin"
            this.userType = UserType.BUSINESS
            this.trustLevel = TrustLevel.TIER_THREE
            this.registrationSource = RegistrationSource.SELF_REGISTRATION
            this.enabled = true
            this.emailVerified = true
            this.merchantId = oauthClient.id
            this.organizationId = organization.id
            this.authorities = mutableSetOf("ROLE_MERCHANT_ADMIN", "ROLE_MERCHANT_SUPER_ADMIN", "ROLE_MERCHANT_USER")
        }
        userRepository.save(user)
    }

    @Nested
    inner class ProcessLifecycle {

        @Test
        fun `should create organization setup process and complete full lifecycle`() {
            val processPublicId = UUID.randomUUID()
            val process = processGateway.createProcess(
                CreateNewProcessPayload(
                    userId = user.id!!,
                    publicId = processPublicId,
                    type = ProcessType.ORGANIZATION_SETUP,
                    description = "Organization setup for ${user.email?.value}",
                    initialState = ProcessState.PENDING,
                    requestState = ProcessState.PENDING,
                    channel = ProcessChannel.WEB_APP,
                    data = mapOf(
                        ProcessRequestDataName.MERCHANT_ID to oauthClient.id.toString(),
                        ProcessRequestDataName.ORGANIZATION_ID to organization.id.toString(),
                        ProcessRequestDataName.USER_IDENTIFIER to user.id.toString()
                    ),
                    stakeholders = mapOf(
                        ProcessStakeholderType.FOR_USER to user.id.toString()
                    )
                )
            )

            assertThat(process.state).isEqualTo(ProcessState.PENDING)
            assertThat(process.type).isEqualTo(ProcessType.ORGANIZATION_SETUP)

            val pendingProcess = processGateway.findLatestPendingProcessesByTypeAndForUserId(
                processType = ProcessType.ORGANIZATION_SETUP,
                userId = user.id!!
            )
            assertThat(pendingProcess).isNotNull
            assertThat(pendingProcess!!.publicId).isEqualTo(processPublicId)

            processGateway.makeRequest(
                MakeProcessRequestPayload(
                    userId = user.id!!,
                    publicId = processPublicId,
                    eventType = ProcessEvent.ORGANIZATION_PROFILE_COMPLETED,
                    requestType = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
                    channel = ProcessChannel.WEB_APP,
                    data = mapOf(
                        ProcessRequestDataName.COMPANY_NAME to "Test Corp",
                        ProcessRequestDataName.SETUP_STEP to "profile"
                    )
                )
            )

            val afterProfile = processGateway.findLatestPendingProcessesByTypeAndForUserId(
                processType = ProcessType.ORGANIZATION_SETUP,
                userId = user.id!!
            )
            assertThat(afterProfile).isNotNull

            val transitions = processGateway.getProcessTransitions(processPublicId)
            assertThat(transitions).anyMatch { it.event == ProcessEvent.ORGANIZATION_PROFILE_COMPLETED }

            val latestOrganization = organizationRepository.findByIdWithProperties(organization.id!!).orElseThrow()
            latestOrganization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "true")
            latestOrganization.status = OrganizationStatus.ACTIVE
            organizationRepository.save(latestOrganization)

            val latestOauthClient = oauthClientRepository.findById(oauthClient.id!!).orElseThrow()
            latestOauthClient.status = OrganizationStatus.ACTIVE
            oauthClientRepository.save(latestOauthClient)

            processGateway.makeRequest(
                MakeProcessRequestPayload(
                    userId = user.id!!,
                    publicId = processPublicId,
                    eventType = ProcessEvent.PROCESS_COMPLETED,
                    requestType = ProcessRequestType.COMPLETE_PROCESS,
                    channel = ProcessChannel.WEB_APP
                )
            )

            val completedProcess = processGateway.findLatestPendingProcessesByTypeAndForUserId(
                processType = ProcessType.ORGANIZATION_SETUP,
                userId = user.id!!
            )
            assertThat(completedProcess).isNull()

            val savedOrganization = organizationRepository.findByIdWithProperties(organization.id!!).orElseThrow()
            assertThat(savedOrganization.getProperty(OrganizationPropertyName.SETUP_COMPLETED)).isEqualTo("true")
            assertThat(savedOrganization.status).isEqualTo(OrganizationStatus.ACTIVE)

            val savedClient = oauthClientRepository.findById(oauthClient.id!!).orElseThrow()
            assertThat(savedClient.status).isEqualTo(OrganizationStatus.ACTIVE)
        }

        @Test
        fun `should keep process pending after profile step`() {
            val processPublicId = UUID.randomUUID()
            processGateway.createProcess(
                CreateNewProcessPayload(
                    userId = user.id!!,
                    publicId = processPublicId,
                    type = ProcessType.ORGANIZATION_SETUP,
                    description = "Organization setup for ${user.email?.value}",
                    initialState = ProcessState.PENDING,
                    requestState = ProcessState.PENDING,
                    channel = ProcessChannel.WEB_APP,
                    data = mapOf(
                        ProcessRequestDataName.USER_IDENTIFIER to user.id.toString()
                    ),
                    stakeholders = mapOf(
                        ProcessStakeholderType.FOR_USER to user.id.toString()
                    )
                )
            )

            processGateway.makeRequest(
                MakeProcessRequestPayload(
                    userId = user.id!!,
                    publicId = processPublicId,
                    eventType = ProcessEvent.ORGANIZATION_PROFILE_COMPLETED,
                    requestType = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
                    channel = ProcessChannel.WEB_APP,
                    data = mapOf(
                        ProcessRequestDataName.SETUP_STEP to "profile"
                    )
                )
            )

            val pendingProcess = processGateway.findLatestPendingProcessesByTypeAndForUserId(
                processType = ProcessType.ORGANIZATION_SETUP,
                userId = user.id!!
            )
            assertThat(pendingProcess).isNotNull
            assertThat(pendingProcess!!.state).isEqualTo(ProcessState.PENDING)
        }
    }
}
