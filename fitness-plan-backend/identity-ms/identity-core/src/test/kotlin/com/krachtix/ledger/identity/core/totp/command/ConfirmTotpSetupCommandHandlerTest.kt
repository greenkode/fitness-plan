package com.krachtix.identity.core.totp.command

import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
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
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeEntity
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeRepository
import com.krachtix.identity.core.totp.service.TotpService
import com.krachtix.commons.dto.Email
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ConfirmTotpSetupCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var totpService: TotpService

    @Mock
    private lateinit var recoveryCodeRepository: TotpRecoveryCodeRepository

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: ConfirmTotpSetupCommandHandler

    private lateinit var user: OAuthUser
    private val userId = UUID.randomUUID()
    private val totpSecret = "JBSWY3DPEHPK3PXP"

    @BeforeEach
    fun setUp() {
        user = OAuthUser(
            username = "testuser",
            password = "encoded",
            email = Email("test@example.com")
        ).apply {
            id = userId
            totpEnabled = false
            totpSecret = this@ConfirmTotpSetupCommandHandlerTest.totpSecret
        }
    }

    @Nested
    inner class Success {

        @Test
        fun `should enable totp and return recovery codes when code is valid`() {
            val recoveryCodes = listOf("ABCD-1234", "EFGH-5678", "IJKL-9012")
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
                type = ProcessType.TOTP_SETUP,
                channel = ProcessChannel.BUSINESS_WEB,
                createdDate = Instant.now(),
                requests = listOf(initialRequest)
            )

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(totpService.generateRecoveryCodes()).thenReturn(recoveryCodes)
            `when`(totpService.hashRecoveryCode(any())).thenReturn("hashed")
            `when`(processGateway.findRecentPendingProcessesByTypeAndForUserId(
                any(), any(), any()
            )).thenReturn(listOf(processDto))
            `when`(messageService.getMessage("totp.setup.confirmed")).thenReturn("TOTP confirmed")

            val command = ConfirmTotpSetupCommand(code = "123456")
            val result = handler.handle(command)

            assertThat(result.recoveryCodes).isEqualTo(recoveryCodes)
            assertThat(result.message).isEqualTo("TOTP confirmed")
            assertThat(user.totpEnabled).isTrue()
        }

        @Test
        fun `should save user with totp enabled`() {
            val recoveryCodes = listOf("ABCD-1234", "EFGH-5678")

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(totpService.generateRecoveryCodes()).thenReturn(recoveryCodes)
            `when`(totpService.hashRecoveryCode(any())).thenReturn("hashed")
            `when`(processGateway.findRecentPendingProcessesByTypeAndForUserId(
                any(), any(), any()
            )).thenReturn(emptyList())
            `when`(messageService.getMessage("totp.setup.confirmed")).thenReturn("TOTP confirmed")

            handler.handle(ConfirmTotpSetupCommand(code = "123456"))

            verify(userRepository).save(user)
        }

        @Test
        fun `should set twoFactorLastVerified when TOTP is enabled`() {
            val recoveryCodes = listOf("ABCD-1234", "EFGH-5678")

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(totpService.generateRecoveryCodes()).thenReturn(recoveryCodes)
            `when`(totpService.hashRecoveryCode(any())).thenReturn("hashed")
            `when`(processGateway.findRecentPendingProcessesByTypeAndForUserId(
                any(), any(), any()
            )).thenReturn(emptyList())
            `when`(messageService.getMessage("totp.setup.confirmed")).thenReturn("TOTP confirmed")

            handler.handle(ConfirmTotpSetupCommand(code = "123456"))

            assertThat(user.twoFactorLastVerified).isNotNull()
        }

        @Test
        fun `should save recovery codes for each generated code`() {
            val recoveryCodes = listOf("ABCD-1234", "EFGH-5678", "IJKL-9012")

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(totpService.generateRecoveryCodes()).thenReturn(recoveryCodes)
            `when`(totpService.hashRecoveryCode(any())).thenReturn("hashed")
            `when`(processGateway.findRecentPendingProcessesByTypeAndForUserId(
                any(), any(), any()
            )).thenReturn(emptyList())
            `when`(messageService.getMessage("totp.setup.confirmed")).thenReturn("TOTP confirmed")

            handler.handle(ConfirmTotpSetupCommand(code = "123456"))

            verify(recoveryCodeRepository, times(3)).save(any(TotpRecoveryCodeEntity::class.java))
        }

        @Test
        fun `should complete pending process when one exists`() {
            val processPublicId = UUID.randomUUID()
            val initialRequest = ProcessRequestDto(
                id = 10L,
                type = ProcessRequestType.CREATE_NEW_PROCESS,
                state = ProcessState.COMPLETE,
                stakeholders = emptyMap(),
                data = emptyMap()
            )
            val processDto = ProcessDto(
                id = 1L,
                publicId = processPublicId,
                state = ProcessState.PENDING,
                type = ProcessType.TOTP_SETUP,
                channel = ProcessChannel.BUSINESS_WEB,
                createdDate = Instant.now(),
                requests = listOf(initialRequest)
            )

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(totpService.generateRecoveryCodes()).thenReturn(listOf("ABCD-1234"))
            `when`(totpService.hashRecoveryCode(any())).thenReturn("hashed")
            `when`(processGateway.findRecentPendingProcessesByTypeAndForUserId(
                any(), any(), any()
            )).thenReturn(listOf(processDto))
            `when`(messageService.getMessage("totp.setup.confirmed")).thenReturn("TOTP confirmed")

            handler.handle(ConfirmTotpSetupCommand(code = "123456"))

            verify(processGateway).completeProcess(processPublicId, 10L)
        }

        @Test
        fun `should not complete process when no pending process exists`() {
            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(totpService.generateRecoveryCodes()).thenReturn(listOf("ABCD-1234"))
            `when`(totpService.hashRecoveryCode(any())).thenReturn("hashed")
            `when`(processGateway.findRecentPendingProcessesByTypeAndForUserId(
                any(), any(), any()
            )).thenReturn(emptyList())
            `when`(messageService.getMessage("totp.setup.confirmed")).thenReturn("TOTP confirmed")

            handler.handle(ConfirmTotpSetupCommand(code = "123456"))

            verify(processGateway, never()).completeProcess(any(), any())
        }
    }

    @Nested
    inner class InvalidCode {

        @Test
        fun `should throw when code is invalid`() {
            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "999999")).thenReturn(false)
            `when`(messageService.getMessage("totp.setup.invalid_code")).thenReturn("Invalid code")

            assertThatThrownBy { handler.handle(ConfirmTotpSetupCommand(code = "999999")) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("Invalid code")

            verify(userRepository, never()).save(any())
        }

        @Test
        fun `should throw when totp secret is null`() {
            user.totpSecret = null

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(messageService.getMessage("totp.setup.invalid_code")).thenReturn("Invalid code")

            assertThatThrownBy { handler.handle(ConfirmTotpSetupCommand(code = "123456")) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("Invalid code")
        }
    }

    @Nested
    inner class AlreadyEnabled {

        @Test
        fun `should throw when totp is already enabled`() {
            user.totpEnabled = true

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(messageService.getMessage("totp.setup.already_enabled")).thenReturn("TOTP already enabled")

            assertThatThrownBy { handler.handle(ConfirmTotpSetupCommand(code = "123456")) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("TOTP already enabled")

            verify(totpService, never()).verifyCode(any(), any())
        }
    }
}
