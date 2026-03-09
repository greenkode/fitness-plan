package com.krachtix.identity.core.settings.query

import com.krachtix.commons.dto.Email
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.ProcessRequestDto
import com.krachtix.commons.process.ProcessTransitionDto
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.OrganizationStatus
import com.krachtix.identity.core.organization.entity.Organization
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetSetupStatusQueryHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @InjectMocks
    private lateinit var handler: GetSetupStatusQueryHandler

    private lateinit var user: OAuthUser
    private lateinit var organization: Organization
    private val userId = UUID.randomUUID()
    private val merchantId = UUID.randomUUID()
    private val processPublicId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        user = OAuthUser(
            username = "test@example.com",
            password = "encoded",
            email = Email("test@example.com")
        ).apply {
            id = userId
            this.merchantId = this@GetSetupStatusQueryHandlerTest.merchantId
        }

        organization = Organization(
            id = merchantId,
            name = "Test Corp",
            slug = "test-corp",
            status = OrganizationStatus.PENDING
        )
    }

    @Test
    fun `should return complete when setup is already completed`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "true")

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))

        val result = handler.handle(GetSetupStatusQuery())

        assertThat(result.isComplete).isTrue()
        assertThat(result.processId).isNull()
        assertThat(result.currentStep).isEqualTo(0)
        assertThat(result.completedSteps).isEmpty()
    }

    @Test
    fun `should return step 0 with org name fallback when no step requests exist`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "false")

        val process = ProcessDto(
            id = 1L,
            publicId = processPublicId,
            state = ProcessState.PENDING,
            type = ProcessType.ORGANIZATION_SETUP,
            channel = ProcessChannel.WEB_APP,
            createdDate = Instant.now(),
            requests = emptyList()
        )

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(process)
        `when`(processGateway.getProcessTransitions(processPublicId)).thenReturn(emptyList())

        val result = handler.handle(GetSetupStatusQuery())

        assertThat(result.isComplete).isFalse()
        assertThat(result.processId).isEqualTo(processPublicId)
        assertThat(result.currentStep).isEqualTo(0)
        assertThat(result.completedSteps).isEmpty()
        assertThat(result.stepData).containsKey("profile")
        assertThat(result.stepData["profile"]?.get("COMPANY_NAME")).isEqualTo("Test Corp")
    }

    @Test
    fun `should return step 1 when profile transition exists`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "false")

        val profileRequest = ProcessRequestDto(
            id = 1L,
            type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
            state = ProcessState.PENDING,
            stakeholders = emptyMap(),
            data = mapOf(
                ProcessRequestDataName.COMPANY_NAME to "Test Corp",
                ProcessRequestDataName.COUNTRY to "US",
                ProcessRequestDataName.SETUP_STEP to "profile"
            )
        )

        val process = ProcessDto(
            id = 1L,
            publicId = processPublicId,
            state = ProcessState.PENDING,
            type = ProcessType.ORGANIZATION_SETUP,
            channel = ProcessChannel.WEB_APP,
            createdDate = Instant.now(),
            requests = listOf(profileRequest)
        )

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(process)
        `when`(processGateway.getProcessTransitions(processPublicId)).thenReturn(
            listOf(
                ProcessTransitionDto(
                    id = 1L,
                    event = ProcessEvent.ORGANIZATION_PROFILE_COMPLETED,
                    userId = userId,
                    oldState = ProcessState.PENDING,
                    newState = ProcessState.PENDING,
                    timestamp = Instant.now()
                )
            )
        )

        val result = handler.handle(GetSetupStatusQuery())

        assertThat(result.isComplete).isFalse()
        assertThat(result.processId).isEqualTo(processPublicId)
        assertThat(result.currentStep).isEqualTo(1)
        assertThat(result.completedSteps).containsExactly("profile")
        assertThat(result.stepData).containsKey("profile")
        assertThat(result.stepData["profile"]?.get("COMPANY_NAME")).isEqualTo("Test Corp")
        assertThat(result.stepData["profile"]?.get("COUNTRY")).isEqualTo("US")
        assertThat(result.stepData["profile"]).doesNotContainKey("SETUP_STEP")
    }

    @Test
    fun `should return all completed steps with step data`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "false")

        val profileRequest = ProcessRequestDto(
            id = 1L,
            type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
            state = ProcessState.PENDING,
            stakeholders = emptyMap(),
            data = mapOf(
                ProcessRequestDataName.COMPANY_NAME to "Test Corp",
                ProcessRequestDataName.SETUP_STEP to "profile"
            )
        )
        val currencyRequest = ProcessRequestDto(
            id = 2L,
            type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
            state = ProcessState.PENDING,
            stakeholders = emptyMap(),
            data = mapOf(
                ProcessRequestDataName.DEFAULT_CURRENCY to "USD",
                ProcessRequestDataName.SETUP_STEP to "currency"
            )
        )
        val preferencesRequest = ProcessRequestDto(
            id = 3L,
            type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
            state = ProcessState.PENDING,
            stakeholders = emptyMap(),
            data = mapOf(
                ProcessRequestDataName.TIMEZONE to "America/New_York",
                ProcessRequestDataName.SETUP_STEP to "preferences"
            )
        )

        val process = ProcessDto(
            id = 1L,
            publicId = processPublicId,
            state = ProcessState.PENDING,
            type = ProcessType.ORGANIZATION_SETUP,
            channel = ProcessChannel.WEB_APP,
            createdDate = Instant.now(),
            requests = listOf(profileRequest, currencyRequest, preferencesRequest)
        )

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(process)
        `when`(processGateway.getProcessTransitions(processPublicId)).thenReturn(
            listOf(
                ProcessTransitionDto(
                    id = 1L,
                    event = ProcessEvent.ORGANIZATION_PROFILE_COMPLETED,
                    userId = userId,
                    oldState = ProcessState.PENDING,
                    newState = ProcessState.PENDING,
                    timestamp = Instant.now()
                ),
                ProcessTransitionDto(
                    id = 2L,
                    event = ProcessEvent.ORGANIZATION_CURRENCY_SELECTED,
                    userId = userId,
                    oldState = ProcessState.PENDING,
                    newState = ProcessState.PENDING,
                    timestamp = Instant.now()
                ),
                ProcessTransitionDto(
                    id = 3L,
                    event = ProcessEvent.ORGANIZATION_PREFERENCES_SAVED,
                    userId = userId,
                    oldState = ProcessState.PENDING,
                    newState = ProcessState.PENDING,
                    timestamp = Instant.now()
                )
            )
        )

        val result = handler.handle(GetSetupStatusQuery())

        assertThat(result.isComplete).isFalse()
        assertThat(result.currentStep).isEqualTo(3)
        assertThat(result.completedSteps).containsExactly("profile", "currency", "preferences")
        assertThat(result.stepData).hasSize(3)
        assertThat(result.stepData["profile"]?.get("COMPANY_NAME")).isEqualTo("Test Corp")
        assertThat(result.stepData["currency"]?.get("DEFAULT_CURRENCY")).isEqualTo("USD")
        assertThat(result.stepData["preferences"]?.get("TIMEZONE")).isEqualTo("America/New_York")
    }

    @Test
    fun `should not override profile step data with organization name when process request exists`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "false")

        val profileRequest = ProcessRequestDto(
            id = 1L,
            type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
            state = ProcessState.PENDING,
            stakeholders = emptyMap(),
            data = mapOf(
                ProcessRequestDataName.COMPANY_NAME to "Updated Corp",
                ProcessRequestDataName.SETUP_STEP to "profile"
            )
        )

        val process = ProcessDto(
            id = 1L,
            publicId = processPublicId,
            state = ProcessState.PENDING,
            type = ProcessType.ORGANIZATION_SETUP,
            channel = ProcessChannel.WEB_APP,
            createdDate = Instant.now(),
            requests = listOf(profileRequest)
        )

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(process)
        `when`(processGateway.getProcessTransitions(processPublicId)).thenReturn(emptyList())

        val result = handler.handle(GetSetupStatusQuery())

        assertThat(result.stepData["profile"]?.get("COMPANY_NAME")).isEqualTo("Updated Corp")
    }

    @Test
    fun `should use latest request when multiple exist for same step`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "false")

        val oldProfileRequest = ProcessRequestDto(
            id = 1L,
            type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
            state = ProcessState.PENDING,
            stakeholders = emptyMap(),
            data = mapOf(
                ProcessRequestDataName.COMPANY_NAME to "Old Corp",
                ProcessRequestDataName.SETUP_STEP to "profile"
            )
        )
        val newProfileRequest = ProcessRequestDto(
            id = 4L,
            type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
            state = ProcessState.PENDING,
            stakeholders = emptyMap(),
            data = mapOf(
                ProcessRequestDataName.COMPANY_NAME to "New Corp",
                ProcessRequestDataName.SETUP_STEP to "profile"
            )
        )

        val process = ProcessDto(
            id = 1L,
            publicId = processPublicId,
            state = ProcessState.PENDING,
            type = ProcessType.ORGANIZATION_SETUP,
            channel = ProcessChannel.WEB_APP,
            createdDate = Instant.now(),
            requests = listOf(oldProfileRequest, newProfileRequest)
        )

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(process)
        `when`(processGateway.getProcessTransitions(processPublicId)).thenReturn(
            listOf(
                ProcessTransitionDto(
                    id = 1L,
                    event = ProcessEvent.ORGANIZATION_PROFILE_COMPLETED,
                    userId = userId,
                    oldState = ProcessState.PENDING,
                    newState = ProcessState.PENDING,
                    timestamp = Instant.now()
                )
            )
        )

        val result = handler.handle(GetSetupStatusQuery())

        assertThat(result.stepData["profile"]?.get("COMPANY_NAME")).isEqualTo("New Corp")
    }
}
