package com.krachtix.identity.core.settings.command

import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.MakeProcessRequestPayload
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
import com.krachtix.identity.core.currency.domain.OrganizationCurrencyRepository
import com.krachtix.identity.core.entity.OAuthRegisteredClient
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.OrganizationStatus
import com.krachtix.identity.core.organization.entity.Organization
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.CacheEvictionService
import com.krachtix.identity.core.service.UserService
import com.krachtix.commons.dto.Email
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CompleteSetupCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var oAuthRegisteredClientRepository: OAuthRegisteredClientRepository

    @Mock
    private lateinit var organizationCurrencyRepository: OrganizationCurrencyRepository

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var cacheEvictionService: CacheEvictionService

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: CompleteSetupCommandHandler

    @Captor
    private lateinit var processRequestCaptor: ArgumentCaptor<MakeProcessRequestPayload>

    private lateinit var user: OAuthUser
    private lateinit var organization: Organization
    private lateinit var client: OAuthRegisteredClient
    private lateinit var processDto: ProcessDto
    private val merchantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val processPublicId = UUID.randomUUID()
    private val oauthClientId = UUID.randomUUID().toString()

    private val allTransitions = listOf(
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

    private val profileRequest = ProcessRequestDto(
        id = 1L,
        type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
        state = ProcessState.PENDING,
        stakeholders = emptyMap(),
        data = mapOf(
            ProcessRequestDataName.COMPANY_NAME to "Test Corp",
            ProcessRequestDataName.INTENDED_PURPOSE to "TECHNOLOGY",
            ProcessRequestDataName.COMPANY_SIZE to "SIZE_11_50",
            ProcessRequestDataName.ROLE_IN_COMPANY to "CEO",
            ProcessRequestDataName.COUNTRY to "US",
            ProcessRequestDataName.PHONE_NUMBER_RAW to "+1 234 567 8900",
            ProcessRequestDataName.WEBSITE_URL to "https://test.com",
            ProcessRequestDataName.SETUP_STEP to "profile"
        )
    )

    private val currencyRequest = ProcessRequestDto(
        id = 2L,
        type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
        state = ProcessState.PENDING,
        stakeholders = emptyMap(),
        data = mapOf(
            ProcessRequestDataName.DEFAULT_CURRENCY to "USD",
            ProcessRequestDataName.MULTI_CURRENCY_ENABLED to "true",
            ProcessRequestDataName.ADDITIONAL_CURRENCIES to "EUR,GBP",
            ProcessRequestDataName.CHART_TEMPLATE_ID to "template-1",
            ProcessRequestDataName.FISCAL_YEAR_START to "01",
            ProcessRequestDataName.SETUP_STEP to "currency"
        )
    )

    private val preferencesRequest = ProcessRequestDto(
        id = 3L,
        type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
        state = ProcessState.PENDING,
        stakeholders = emptyMap(),
        data = mapOf(
            ProcessRequestDataName.TIMEZONE to "America/New_York",
            ProcessRequestDataName.DATE_FORMAT to "MM/DD/YYYY",
            ProcessRequestDataName.NUMBER_FORMAT to "1,234.56",
            ProcessRequestDataName.SETUP_STEP to "preferences"
        )
    )

    @BeforeEach
    fun setUp() {
        user = OAuthUser(
            username = "test@example.com",
            password = "encoded",
            email = Email("test@example.com")
        ).apply {
            id = userId
            this.merchantId = this@CompleteSetupCommandHandlerTest.merchantId
        }

        organization = Organization(
            id = merchantId,
            name = "-",
            slug = "test-corp",
            status = OrganizationStatus.PENDING
        )

        client = OAuthRegisteredClient().apply {
            id = merchantId
            clientId = oauthClientId
            clientName = "-"
        }

        processDto = ProcessDto(
            id = 1L,
            publicId = processPublicId,
            state = ProcessState.PENDING,
            type = ProcessType.ORGANIZATION_SETUP,
            channel = ProcessChannel.WEB_APP,
            createdDate = Instant.now(),
            requests = listOf(profileRequest, currencyRequest, preferencesRequest)
        )
    }

    @Test
    fun `should complete setup and persist all step data`() {
        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(oAuthRegisteredClientRepository.findById(merchantId)).thenReturn(Optional.of(client))
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(processDto)
        `when`(processGateway.getProcessTransitions(processPublicId)).thenReturn(allTransitions)
        `when`(organizationCurrencyRepository.findByClientId(oauthClientId)).thenReturn(emptyList())
        `when`(messageService.getMessage("setup.success.completed")).thenReturn("Setup completed")

        val result = handler.handle(CompleteSetupCommand())

        assertThat(result.success).isTrue()
        assertThat(result.merchantId).isEqualTo(merchantId.toString())

        assertThat(organization.name).isEqualTo("Test Corp")
        assertThat(organization.getProperty(OrganizationPropertyName.INTENDED_PURPOSE)).isEqualTo("TECHNOLOGY")
        assertThat(organization.getProperty(OrganizationPropertyName.COMPANY_SIZE)).isEqualTo("SIZE_11_50")
        assertThat(organization.getProperty(OrganizationPropertyName.ROLE_IN_COMPANY)).isEqualTo("CEO")
        assertThat(organization.getProperty(OrganizationPropertyName.COUNTRY)).isEqualTo("US")
        assertThat(organization.getProperty(OrganizationPropertyName.PHONE_NUMBER)).isEqualTo("+1 234 567 8900")
        assertThat(organization.getProperty(OrganizationPropertyName.WEBSITE)).isEqualTo("https://test.com")
        assertThat(organization.getProperty(OrganizationPropertyName.TERMS_ACCEPTED)).isEqualTo("true")

        assertThat(organization.getProperty(OrganizationPropertyName.DEFAULT_CURRENCY)).isEqualTo("USD")
        assertThat(organization.getProperty(OrganizationPropertyName.MULTI_CURRENCY_ENABLED)).isEqualTo("true")
        assertThat(organization.getProperty(OrganizationPropertyName.CHART_TEMPLATE_ID)).isEqualTo("template-1")
        assertThat(organization.getProperty(OrganizationPropertyName.FISCAL_YEAR_START)).isEqualTo("01")

        assertThat(organization.getProperty(OrganizationPropertyName.TIMEZONE)).isEqualTo("America/New_York")
        assertThat(organization.getProperty(OrganizationPropertyName.DATE_FORMAT)).isEqualTo("MM/DD/YYYY")
        assertThat(organization.getProperty(OrganizationPropertyName.NUMBER_FORMAT)).isEqualTo("1,234.56")

        assertThat(organization.getProperty(OrganizationPropertyName.SETUP_COMPLETED)).isEqualTo("true")
        assertThat(organization.status).isEqualTo(OrganizationStatus.ACTIVE)
        assertThat(client.status).isEqualTo(OrganizationStatus.ACTIVE)
        assertThat(client.clientName).isEqualTo("Test Corp")

        verify(organizationRepository).save(organization)
        verify(oAuthRegisteredClientRepository).save(client)
        verify(cacheEvictionService).evictMerchantCaches(merchantId.toString())

        verify(processGateway).makeRequest(capture(processRequestCaptor))
        val request = processRequestCaptor.value
        assertThat(request.publicId).isEqualTo(processPublicId)
        assertThat(request.eventType).isEqualTo(ProcessEvent.PROCESS_COMPLETED)
        assertThat(request.requestType).isEqualTo(ProcessRequestType.COMPLETE_PROCESS)
    }

    @Test
    fun `should use latest request when multiple exist for same step`() {
        val updatedProfileRequest = ProcessRequestDto(
            id = 4L,
            type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
            state = ProcessState.PENDING,
            stakeholders = emptyMap(),
            data = mapOf(
                ProcessRequestDataName.COMPANY_NAME to "Updated Corp",
                ProcessRequestDataName.INTENDED_PURPOSE to "FINANCE",
                ProcessRequestDataName.COMPANY_SIZE to "SIZE_51_200",
                ProcessRequestDataName.ROLE_IN_COMPANY to "CFO",
                ProcessRequestDataName.COUNTRY to "GB",
                ProcessRequestDataName.PHONE_NUMBER_RAW to "+44 20 1234 5678",
                ProcessRequestDataName.SETUP_STEP to "profile"
            )
        )
        val processDtoWithUpdated = processDto.copy(
            requests = listOf(profileRequest, currencyRequest, preferencesRequest, updatedProfileRequest)
        )

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(oAuthRegisteredClientRepository.findById(merchantId)).thenReturn(Optional.of(client))
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(processDtoWithUpdated)
        `when`(processGateway.getProcessTransitions(processPublicId)).thenReturn(allTransitions)
        `when`(organizationCurrencyRepository.findByClientId(oauthClientId)).thenReturn(emptyList())
        `when`(messageService.getMessage("setup.success.completed")).thenReturn("Setup completed")

        handler.handle(CompleteSetupCommand())

        assertThat(organization.name).isEqualTo("Updated Corp")
        assertThat(organization.getProperty(OrganizationPropertyName.INTENDED_PURPOSE)).isEqualTo("FINANCE")
        assertThat(organization.getProperty(OrganizationPropertyName.COUNTRY)).isEqualTo("GB")
    }

    private fun <T> capture(captor: ArgumentCaptor<T>): T = captor.capture()

    @Test
    fun `should throw when profile step not completed`() {
        val processDtoNoRequests = processDto.copy(requests = emptyList())

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(processDtoNoRequests)
        `when`(processGateway.getProcessTransitions(processPublicId)).thenReturn(emptyList())
        `when`(messageService.getMessage("setup.error.profile_not_completed")).thenReturn("Profile not completed")

        assertThatThrownBy { handler.handle(CompleteSetupCommand()) }
            .isInstanceOf(InvalidRequestException::class.java)
            .hasMessage("Profile not completed")
    }

    @Test
    fun `should throw when currency step not completed`() {
        val processDtoProfileOnly = processDto.copy(requests = listOf(profileRequest))
        val profileOnlyTransitions = listOf(allTransitions[0])

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(processDtoProfileOnly)
        `when`(processGateway.getProcessTransitions(processPublicId)).thenReturn(profileOnlyTransitions)
        `when`(messageService.getMessage("setup.error.currency_not_completed")).thenReturn("Currency not completed")

        assertThatThrownBy { handler.handle(CompleteSetupCommand()) }
            .isInstanceOf(InvalidRequestException::class.java)
            .hasMessage("Currency not completed")
    }

    @Test
    fun `should throw when preferences step not completed`() {
        val processDtoNoPrefs = processDto.copy(requests = listOf(profileRequest, currencyRequest))
        val noPrefTransitions = listOf(allTransitions[0], allTransitions[1])

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(processDtoNoPrefs)
        `when`(processGateway.getProcessTransitions(processPublicId)).thenReturn(noPrefTransitions)
        `when`(messageService.getMessage("setup.error.preferences_not_completed")).thenReturn("Preferences not completed")

        assertThatThrownBy { handler.handle(CompleteSetupCommand()) }
            .isInstanceOf(InvalidRequestException::class.java)
            .hasMessage("Preferences not completed")
    }

    @Test
    fun `should throw when no pending process found`() {
        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(null)
        `when`(messageService.getMessage("setup.error.no_pending_process")).thenReturn("No pending process")

        assertThatThrownBy { handler.handle(CompleteSetupCommand()) }
            .isInstanceOf(RecordNotFoundException::class.java)
            .hasMessage("No pending process")
    }

    @Test
    fun `should throw when user has no merchant`() {
        val userWithoutMerchant = OAuthUser(
            username = "test@example.com",
            password = "encoded",
            email = Email("test@example.com")
        ).apply {
            id = userId
            merchantId = null
        }

        `when`(userService.getCurrentUser()).thenReturn(userWithoutMerchant)
        `when`(messageService.getMessage("settings.error.no_merchant")).thenReturn("No merchant")

        assertThatThrownBy { handler.handle(CompleteSetupCommand()) }
            .isInstanceOf(RecordNotFoundException::class.java)
            .hasMessage("No merchant")
    }
}
