package com.krachtix.identity.core.totp.command

import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.ProcessRequestDto
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeRepository
import com.krachtix.identity.core.totp.service.TotpService
import com.krachtix.commons.dto.Email
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class DisableTotpCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var totpService: TotpService

    @Mock
    private lateinit var recoveryCodeRepository: TotpRecoveryCodeRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: DisableTotpCommandHandler

    @Captor
    private lateinit var processPayloadCaptor: ArgumentCaptor<CreateNewProcessPayload>

    private lateinit var user: OAuthUser
    private val userId = UUID.randomUUID()
    private val totpSecret = "JBSWY3DPEHPK3PXP"

    @BeforeEach
    fun setUp() {
        user = OAuthUser(
            username = "testuser",
            password = "encoded-password",
            email = Email("test@example.com")
        ).apply {
            id = userId
            totpEnabled = true
            totpSecret = this@DisableTotpCommandHandlerTest.totpSecret
        }
    }

    @Nested
    inner class DisableViaTotpCode {

        @Test
        fun `should disable totp when valid totp code is provided`() {
            val processPublicId = UUID.randomUUID()
            val initialRequest = ProcessRequestDto(
                id = 1L,
                type = ProcessRequestType.CREATE_NEW_PROCESS,
                state = ProcessState.COMPLETE,
                stakeholders = emptyMap(),
                data = emptyMap()
            )
            val processDto = ProcessDto(
                id = 1L,
                publicId = processPublicId,
                state = ProcessState.PENDING,
                type = ProcessType.TOTP_DISABLE,
                channel = ProcessChannel.BUSINESS_WEB,
                createdDate = Instant.now(),
                requests = listOf(initialRequest)
            )

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(processGateway.createProcess(any())).thenReturn(processDto)
            `when`(processGateway.findPendingProcessByPublicId(any())).thenReturn(processDto)
            `when`(messageService.getMessage("totp.disable.success")).thenReturn("TOTP disabled")

            val result = handler.handle(DisableTotpCommand(code = "123456"))

            assertThat(result.message).isEqualTo("TOTP disabled")
            assertThat(user.totpEnabled).isFalse()
            assertThat(user.totpSecret).isNull()
        }

        @Test
        fun `should delete recovery codes when disabling`() {
            val processPublicId = UUID.randomUUID()
            val initialRequest = ProcessRequestDto(
                id = 1L,
                type = ProcessRequestType.CREATE_NEW_PROCESS,
                state = ProcessState.COMPLETE,
                stakeholders = emptyMap(),
                data = emptyMap()
            )
            val processDto = ProcessDto(
                id = 1L,
                publicId = processPublicId,
                state = ProcessState.PENDING,
                type = ProcessType.TOTP_DISABLE,
                channel = ProcessChannel.BUSINESS_WEB,
                createdDate = Instant.now(),
                requests = listOf(initialRequest)
            )

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(processGateway.createProcess(any())).thenReturn(processDto)
            `when`(processGateway.findPendingProcessByPublicId(any())).thenReturn(processDto)
            `when`(messageService.getMessage("totp.disable.success")).thenReturn("TOTP disabled")

            handler.handle(DisableTotpCommand(code = "123456"))

            verify(recoveryCodeRepository).deleteByUserId(userId)
        }

        @Test
        fun `should create a process for totp disable`() {
            val processPublicId = UUID.randomUUID()
            val initialRequest = ProcessRequestDto(
                id = 1L,
                type = ProcessRequestType.CREATE_NEW_PROCESS,
                state = ProcessState.COMPLETE,
                stakeholders = emptyMap(),
                data = emptyMap()
            )
            val processDto = ProcessDto(
                id = 1L,
                publicId = processPublicId,
                state = ProcessState.PENDING,
                type = ProcessType.TOTP_DISABLE,
                channel = ProcessChannel.BUSINESS_WEB,
                createdDate = Instant.now(),
                requests = listOf(initialRequest)
            )

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(processGateway.createProcess(capture(processPayloadCaptor))).thenReturn(processDto)
            `when`(processGateway.findPendingProcessByPublicId(any())).thenReturn(processDto)
            `when`(messageService.getMessage("totp.disable.success")).thenReturn("TOTP disabled")

            handler.handle(DisableTotpCommand(code = "123456"))

            val payload = processPayloadCaptor.value
            assertThat(payload.userId).isEqualTo(userId)
            assertThat(payload.type).isEqualTo(ProcessType.TOTP_DISABLE)
            assertThat(payload.initialState).isEqualTo(ProcessState.PENDING)
            assertThat(payload.requestState).isEqualTo(ProcessState.COMPLETE)
            assertThat(payload.channel).isEqualTo(ProcessChannel.BUSINESS_WEB)
        }
    }

    @Nested
    inner class DisableViaPassword {

        @Test
        fun `should disable totp when valid password is provided`() {
            val processPublicId = UUID.randomUUID()
            val initialRequest = ProcessRequestDto(
                id = 1L,
                type = ProcessRequestType.CREATE_NEW_PROCESS,
                state = ProcessState.COMPLETE,
                stakeholders = emptyMap(),
                data = emptyMap()
            )
            val processDto = ProcessDto(
                id = 1L,
                publicId = processPublicId,
                state = ProcessState.PENDING,
                type = ProcessType.TOTP_DISABLE,
                channel = ProcessChannel.BUSINESS_WEB,
                createdDate = Instant.now(),
                requests = listOf(initialRequest)
            )

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(passwordEncoder.matches("raw-password", "encoded-password")).thenReturn(true)
            `when`(processGateway.createProcess(any())).thenReturn(processDto)
            `when`(processGateway.findPendingProcessByPublicId(any())).thenReturn(processDto)
            `when`(messageService.getMessage("totp.disable.success")).thenReturn("TOTP disabled")

            val result = handler.handle(DisableTotpCommand(password = "raw-password"))

            assertThat(result.message).isEqualTo("TOTP disabled")
            assertThat(user.totpEnabled).isFalse()
            assertThat(user.totpSecret).isNull()
            verify(userRepository).save(user)
        }
    }

    @Nested
    inner class NotEnabled {

        @Test
        fun `should throw when totp is not enabled`() {
            user.totpEnabled = false

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(messageService.getMessage("totp.disable.not_enabled")).thenReturn("TOTP not enabled")

            assertThatThrownBy { handler.handle(DisableTotpCommand(code = "123456")) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("TOTP not enabled")

            verify(userRepository, never()).save(any())
        }
    }

    @Nested
    inner class InvalidVerification {

        @Test
        fun `should throw when totp code is invalid`() {
            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "999999")).thenReturn(false)
            `when`(messageService.getMessage("totp.disable.invalid_verification")).thenReturn("Invalid verification")

            assertThatThrownBy { handler.handle(DisableTotpCommand(code = "999999")) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("Invalid verification")
        }

        @Test
        fun `should throw when password is invalid`() {
            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false)
            `when`(messageService.getMessage("totp.disable.invalid_verification")).thenReturn("Invalid verification")

            assertThatThrownBy { handler.handle(DisableTotpCommand(password = "wrong-password")) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("Invalid verification")
        }

        @Test
        fun `should throw when neither code nor password is provided`() {
            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(messageService.getMessage("totp.disable.invalid_verification")).thenReturn("Invalid verification")

            assertThatThrownBy { handler.handle(DisableTotpCommand()) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("Invalid verification")
        }

        @Test
        fun `should throw when totp secret is null and code is provided`() {
            user.totpSecret = null

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(messageService.getMessage("totp.disable.invalid_verification")).thenReturn("Invalid verification")

            assertThatThrownBy { handler.handle(DisableTotpCommand(code = "123456")) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("Invalid verification")
        }
    }

    private fun <T> capture(captor: ArgumentCaptor<T>): T = captor.capture()
}
