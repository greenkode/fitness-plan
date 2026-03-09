package com.krachtix.identity.core.totp.command

import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.totp.service.TotpService
import com.krachtix.commons.dto.Email
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InitiateTotpSetupCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var totpService: TotpService

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: InitiateTotpSetupCommandHandler

    @Captor
    private lateinit var processPayloadCaptor: ArgumentCaptor<CreateNewProcessPayload>

    private lateinit var user: OAuthUser
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        user = OAuthUser(
            username = "testuser",
            password = "encoded",
            email = Email("test@example.com")
        ).apply {
            id = userId
            totpEnabled = false
            totpSecret = null
        }
    }

    @Nested
    inner class Success {

        @Test
        fun `should return secret and qr code uri when totp not already enabled`() {
            val generatedSecret = "JBSWY3DPEHPK3PXP"
            val expectedQrUri = "otpauth://totp/Krachtix:test@example.com?secret=$generatedSecret&issuer=Krachtix&algorithm=SHA1&digits=6&period=30"

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.generateSecret()).thenReturn(generatedSecret)
            `when`(totpService.generateQrCodeUri(generatedSecret, "test@example.com")).thenReturn(expectedQrUri)
            `when`(messageService.getMessage("totp.setup.initiated")).thenReturn("TOTP setup initiated")

            val result = handler.handle(InitiateTotpSetupCommand())

            assertThat(result.secret).isEqualTo(generatedSecret)
            assertThat(result.qrCodeUri).isEqualTo(expectedQrUri)
            assertThat(result.message).isEqualTo("TOTP setup initiated")
        }

        @Test
        fun `should save secret to user entity`() {
            val generatedSecret = "JBSWY3DPEHPK3PXP"

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.generateSecret()).thenReturn(generatedSecret)
            `when`(totpService.generateQrCodeUri(generatedSecret, "test@example.com")).thenReturn("otpauth://totp/...")
            `when`(messageService.getMessage("totp.setup.initiated")).thenReturn("TOTP setup initiated")

            handler.handle(InitiateTotpSetupCommand())

            assertThat(user.totpSecret).isEqualTo(generatedSecret)
            verify(userRepository).save(user)
        }

        @Test
        fun `should create a process for totp setup`() {
            val generatedSecret = "JBSWY3DPEHPK3PXP"

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.generateSecret()).thenReturn(generatedSecret)
            `when`(totpService.generateQrCodeUri(generatedSecret, "test@example.com")).thenReturn("otpauth://totp/...")
            `when`(messageService.getMessage("totp.setup.initiated")).thenReturn("TOTP setup initiated")

            handler.handle(InitiateTotpSetupCommand())

            verify(processGateway).createProcess(capture(processPayloadCaptor))
            val payload = processPayloadCaptor.value
            assertThat(payload.userId).isEqualTo(userId)
            assertThat(payload.type).isEqualTo(ProcessType.TOTP_SETUP)
            assertThat(payload.initialState).isEqualTo(ProcessState.PENDING)
            assertThat(payload.requestState).isEqualTo(ProcessState.COMPLETE)
            assertThat(payload.channel).isEqualTo(ProcessChannel.BUSINESS_WEB)
        }

        @Test
        fun `should use username when email is null`() {
            user.email = null
            val generatedSecret = "JBSWY3DPEHPK3PXP"

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.generateSecret()).thenReturn(generatedSecret)
            `when`(totpService.generateQrCodeUri(generatedSecret, "testuser")).thenReturn("otpauth://totp/...")
            `when`(messageService.getMessage("totp.setup.initiated")).thenReturn("TOTP setup initiated")

            handler.handle(InitiateTotpSetupCommand())

            verify(totpService).generateQrCodeUri(generatedSecret, "testuser")
        }
    }

    @Nested
    inner class AlreadyEnabled {

        @Test
        fun `should throw when totp is already enabled`() {
            user.totpEnabled = true

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(messageService.getMessage("totp.setup.already_enabled")).thenReturn("TOTP already enabled")

            assertThatThrownBy { handler.handle(InitiateTotpSetupCommand()) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("TOTP already enabled")

            verify(totpService, never()).generateSecret()
            verify(userRepository, never()).save(user)
        }
    }

    private fun <T> capture(captor: ArgumentCaptor<T>): T = captor.capture()
}
