package com.krachtix.identity.core.settings.command

import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.MakeProcessRequestPayload
import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.entity.CompanyRole
import com.krachtix.identity.core.entity.CompanySize
import com.krachtix.identity.core.entity.IntendedPurpose
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.service.UserService
import com.krachtix.commons.dto.Email
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.ProcessRequestDto
import com.krachtix.commons.process.enumeration.ProcessState
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
class SaveBusinessProfileCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: SaveBusinessProfileCommandHandler

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
            this.merchantId = this@SaveBusinessProfileCommandHandlerTest.merchantId
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
        `when`(messageService.getMessage("setup.success.profile_saved")).thenReturn("Business profile saved successfully")

        val command = SaveBusinessProfileCommand(
            companyName = "Test Corp",
            intendedPurpose = IntendedPurpose.TECHNOLOGY,
            companySize = CompanySize.SIZE_11_50,
            roleInCompany = CompanyRole.CEO,
            country = "US",
            phoneNumber = "+1 234 567 8900",
            parsedPhoneNumber = null,
            website = "https://test.com",
            termsAccepted = true
        )

        val result = handler.handle(command)

        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo("Business profile saved successfully")

        verify(processGateway).makeRequest(capture(processRequestCaptor))

        val request = processRequestCaptor.value
        assertThat(request.publicId).isEqualTo(processPublicId)
        assertThat(request.eventType).isEqualTo(ProcessEvent.ORGANIZATION_PROFILE_COMPLETED)
        assertThat(request.requestType).isEqualTo(ProcessRequestType.ORGANIZATION_STEP_UPDATE)
        assertThat(request.data[ProcessRequestDataName.COMPANY_NAME]).isEqualTo("Test Corp")
        assertThat(request.data[ProcessRequestDataName.INTENDED_PURPOSE]).isEqualTo("TECHNOLOGY")
        assertThat(request.data[ProcessRequestDataName.COMPANY_SIZE]).isEqualTo("SIZE_11_50")
        assertThat(request.data[ProcessRequestDataName.ROLE_IN_COMPANY]).isEqualTo("CEO")
        assertThat(request.data[ProcessRequestDataName.COUNTRY]).isEqualTo("US")
        assertThat(request.data[ProcessRequestDataName.PHONE_NUMBER_RAW]).isEqualTo("+1 234 567 8900")
        assertThat(request.data[ProcessRequestDataName.WEBSITE_URL]).isEqualTo("https://test.com")
        assertThat(request.data[ProcessRequestDataName.SETUP_STEP]).isEqualTo("profile")
    }

    @Test
    fun `should omit website url when blank`() {
        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(processDto)
        `when`(messageService.getMessage("setup.success.profile_saved")).thenReturn("Business profile saved successfully")

        val command = SaveBusinessProfileCommand(
            companyName = "Test Corp",
            intendedPurpose = IntendedPurpose.TECHNOLOGY,
            companySize = CompanySize.SIZE_11_50,
            roleInCompany = CompanyRole.CEO,
            country = "US",
            phoneNumber = "+1 234 567 8900",
            parsedPhoneNumber = null,
            website = "https://",
            termsAccepted = true
        )

        handler.handle(command)

        verify(processGateway).makeRequest(capture(processRequestCaptor))
        assertThat(processRequestCaptor.value.data).doesNotContainKey(ProcessRequestDataName.WEBSITE_URL)
    }

    @Test
    fun `should always create new request even when step already recorded`() {
        val existingRequest = ProcessRequestDto(
            id = 1L,
            type = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
            state = ProcessState.PENDING,
            stakeholders = emptyMap(),
            data = mapOf(ProcessRequestDataName.SETUP_STEP to "profile")
        )
        val processDtoWithRequest = processDto.copy(requests = listOf(existingRequest))

        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(processDtoWithRequest)
        `when`(messageService.getMessage("setup.success.profile_saved")).thenReturn("Business profile saved successfully")

        val command = SaveBusinessProfileCommand(
            companyName = "Updated Corp",
            intendedPurpose = IntendedPurpose.TECHNOLOGY,
            companySize = CompanySize.SIZE_11_50,
            roleInCompany = CompanyRole.CEO,
            country = "US",
            phoneNumber = "+1 234 567 8900",
            parsedPhoneNumber = null,
            website = "https://test.com",
            termsAccepted = true
        )

        val result = handler.handle(command)

        assertThat(result.success).isTrue()
        verify(processGateway).makeRequest(capture(processRequestCaptor))
        assertThat(processRequestCaptor.value.data[ProcessRequestDataName.COMPANY_NAME]).isEqualTo("Updated Corp")
    }

    @Test
    fun `should throw when terms not accepted`() {
        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(messageService.getMessage("settings.error.terms_required")).thenReturn("Terms required")

        val command = SaveBusinessProfileCommand(
            companyName = "Test Corp",
            intendedPurpose = IntendedPurpose.TECHNOLOGY,
            companySize = CompanySize.SIZE_11_50,
            roleInCompany = CompanyRole.CEO,
            country = "US",
            phoneNumber = "+1 234 567 8900",
            parsedPhoneNumber = null,
            website = null,
            termsAccepted = false
        )

        assertThatThrownBy { handler.handle(command) }
            .isInstanceOf(InvalidRequestException::class.java)
            .hasMessage("Terms required")
    }

    @Test
    fun `should throw when no pending process found`() {
        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.ORGANIZATION_SETUP, userId))
            .thenReturn(null)
        `when`(messageService.getMessage("setup.error.no_pending_process")).thenReturn("No pending process")

        val command = SaveBusinessProfileCommand(
            companyName = "Test Corp",
            intendedPurpose = IntendedPurpose.TECHNOLOGY,
            companySize = CompanySize.SIZE_11_50,
            roleInCompany = CompanyRole.CEO,
            country = "US",
            phoneNumber = "+1 234 567 8900",
            parsedPhoneNumber = null,
            website = null,
            termsAccepted = true
        )

        assertThatThrownBy { handler.handle(command) }
            .isInstanceOf(RecordNotFoundException::class.java)
            .hasMessage("No pending process")
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

        val command = SaveBusinessProfileCommand(
            companyName = "Test Corp",
            intendedPurpose = IntendedPurpose.TECHNOLOGY,
            companySize = CompanySize.SIZE_11_50,
            roleInCompany = CompanyRole.CEO,
            country = "US",
            phoneNumber = "+1 234 567 8900",
            parsedPhoneNumber = null,
            website = null,
            termsAccepted = true
        )

        assertThatThrownBy { handler.handle(command) }
            .isInstanceOf(RecordNotFoundException::class.java)
            .hasMessage("No merchant")
    }
}
