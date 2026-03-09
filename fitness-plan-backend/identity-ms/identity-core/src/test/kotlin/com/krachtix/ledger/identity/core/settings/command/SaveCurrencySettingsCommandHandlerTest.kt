package com.krachtix.identity.core.settings.command

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.MakeProcessRequestPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.ProcessRequestDto
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.service.UserService
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
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SaveCurrencySettingsCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: SaveCurrencySettingsCommandHandler

    @Captor
    private lateinit var processRequestCaptor: ArgumentCaptor<MakeProcessRequestPayload>

    private lateinit var user: OAuthUser
    private lateinit var processDto: ProcessDto
    private val merchantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val processPublicId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        user = OAuthUser(
            username = "test@example.com",
            password = "encoded",
            email = Email("test@example.com")
        ).apply {
            id = userId
            this.merchantId = this@SaveCurrencySettingsCommandHandlerTest.merchantId
        }

        processDto = ProcessDto(
            id = 1L,
            publicId = processPublicId,
            state = ProcessState.PENDING,
            type = ProcessType.ORGANIZATION_SETUP,
            channel = ProcessChannel.WEB_APP,
            createdDate = Instant.now(),
            requests = emptyList()
        )
    }

    @Test
    fun `should record process request with correct data`() {
        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(processDto)
        `when`(messageService.getMessage("setup.success.currency_saved")).thenReturn("Currency settings saved")

        val command = SaveCurrencySettingsCommand(
            primaryCurrency = "USD",
            multiCurrencyEnabled = true,
            additionalCurrencies = listOf("EUR", "GBP"),
            chartTemplateId = "template-1",
            fiscalYearStart = "01"
        )

        val result = handler.handle(command)

        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo("Currency settings saved")

        verify(processGateway).makeRequest(capture(processRequestCaptor))

        val request = processRequestCaptor.value
        assertThat(request.publicId).isEqualTo(processPublicId)
        assertThat(request.eventType).isEqualTo(ProcessEvent.ORGANIZATION_CURRENCY_SELECTED)
        assertThat(request.requestType).isEqualTo(ProcessRequestType.ORGANIZATION_STEP_UPDATE)
        assertThat(request.data[ProcessRequestDataName.DEFAULT_CURRENCY]).isEqualTo("USD")
        assertThat(request.data[ProcessRequestDataName.MULTI_CURRENCY_ENABLED]).isEqualTo("true")
        assertThat(request.data[ProcessRequestDataName.ADDITIONAL_CURRENCIES]).isEqualTo("EUR,GBP")
        assertThat(request.data[ProcessRequestDataName.CHART_TEMPLATE_ID]).isEqualTo("template-1")
        assertThat(request.data[ProcessRequestDataName.FISCAL_YEAR_START]).isEqualTo("01")
        assertThat(request.data[ProcessRequestDataName.SETUP_STEP]).isEqualTo("currency")
    }

    @Test
    fun `should omit additional currencies when multi-currency disabled`() {
        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(processDto)
        `when`(messageService.getMessage("setup.success.currency_saved")).thenReturn("Currency settings saved")

        val command = SaveCurrencySettingsCommand(
            primaryCurrency = "EUR",
            multiCurrencyEnabled = false,
            additionalCurrencies = emptyList(),
            chartTemplateId = "template-2",
            fiscalYearStart = "04"
        )

        handler.handle(command)

        verify(processGateway).makeRequest(capture(processRequestCaptor))
        assertThat(processRequestCaptor.value.data).doesNotContainKey(ProcessRequestDataName.ADDITIONAL_CURRENCIES)
        assertThat(processRequestCaptor.value.data[ProcessRequestDataName.MULTI_CURRENCY_ENABLED]).isEqualTo("false")
    }

    @Test
    fun `should always create new request even when step already recorded`() {
        val existingRequest = ProcessRequestDto(
            id = 1L,
            type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
            state = ProcessState.PENDING,
            stakeholders = emptyMap(),
            data = mapOf(ProcessRequestDataName.SETUP_STEP to "currency")
        )
        val processDtoWithRequest = processDto.copy(requests = listOf(existingRequest))

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(processDtoWithRequest)
        `when`(messageService.getMessage("setup.success.currency_saved")).thenReturn("Currency settings saved")

        val command = SaveCurrencySettingsCommand(
            primaryCurrency = "GBP",
            multiCurrencyEnabled = false,
            additionalCurrencies = emptyList(),
            chartTemplateId = "template-3",
            fiscalYearStart = "01"
        )

        val result = handler.handle(command)

        assertThat(result.success).isTrue()
        verify(processGateway).makeRequest(capture(processRequestCaptor))
        assertThat(processRequestCaptor.value.data[ProcessRequestDataName.DEFAULT_CURRENCY]).isEqualTo("GBP")
    }

    private fun <T> capture(captor: ArgumentCaptor<T>): T = captor.capture()

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

        val command = SaveCurrencySettingsCommand(
            primaryCurrency = "USD",
            multiCurrencyEnabled = false,
            additionalCurrencies = emptyList(),
            chartTemplateId = "template-1",
            fiscalYearStart = "01"
        )

        assertThatThrownBy { handler.handle(command) }
            .isInstanceOf(RecordNotFoundException::class.java)
            .hasMessage("No merchant")
    }

    @Test
    fun `should throw when no pending process found`() {
        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(null)
        `when`(messageService.getMessage("setup.error.no_pending_process")).thenReturn("No pending process")

        val command = SaveCurrencySettingsCommand(
            primaryCurrency = "USD",
            multiCurrencyEnabled = false,
            additionalCurrencies = emptyList(),
            chartTemplateId = "template-1",
            fiscalYearStart = "01"
        )

        assertThatThrownBy { handler.handle(command) }
            .isInstanceOf(RecordNotFoundException::class.java)
            .hasMessage("No pending process")
    }
}
