package com.krachtix.identity.core.totp.command

import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.entity.OAuthUser
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
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RegenerateRecoveryCodesCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var totpService: TotpService

    @Mock
    private lateinit var recoveryCodeRepository: TotpRecoveryCodeRepository

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: RegenerateRecoveryCodesCommandHandler

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
            totpEnabled = true
            totpSecret = this@RegenerateRecoveryCodesCommandHandlerTest.totpSecret
        }
    }

    @Nested
    inner class Success {

        @Test
        fun `should regenerate recovery codes and return them`() {
            val newCodes = listOf("AAAA-1111", "BBBB-2222", "CCCC-3333")

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(totpService.generateRecoveryCodes()).thenReturn(newCodes)
            `when`(totpService.hashRecoveryCode(any())).thenReturn("hashed")
            `when`(messageService.getMessage("totp.recovery.regenerated")).thenReturn("Recovery codes regenerated")

            val result = handler.handle(RegenerateRecoveryCodesCommand(code = "123456"))

            assertThat(result.recoveryCodes).isEqualTo(newCodes)
            assertThat(result.message).isEqualTo("Recovery codes regenerated")
        }

        @Test
        fun `should delete old recovery codes before generating new ones`() {
            val newCodes = listOf("AAAA-1111", "BBBB-2222")

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(totpService.generateRecoveryCodes()).thenReturn(newCodes)
            `when`(totpService.hashRecoveryCode(any())).thenReturn("hashed")
            `when`(messageService.getMessage("totp.recovery.regenerated")).thenReturn("Recovery codes regenerated")

            handler.handle(RegenerateRecoveryCodesCommand(code = "123456"))

            verify(recoveryCodeRepository).deleteByUserId(userId)
        }

        @Test
        fun `should save each new recovery code`() {
            val newCodes = listOf("AAAA-1111", "BBBB-2222", "CCCC-3333", "DDDD-4444")

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            `when`(totpService.generateRecoveryCodes()).thenReturn(newCodes)
            `when`(totpService.hashRecoveryCode(any())).thenReturn("hashed")
            `when`(messageService.getMessage("totp.recovery.regenerated")).thenReturn("Recovery codes regenerated")

            handler.handle(RegenerateRecoveryCodesCommand(code = "123456"))

            verify(recoveryCodeRepository, times(4)).save(any(TotpRecoveryCodeEntity::class.java))
        }
    }

    @Nested
    inner class TotpNotEnabled {

        @Test
        fun `should throw when totp is not enabled`() {
            user.totpEnabled = false

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(messageService.getMessage("totp.disable.not_enabled")).thenReturn("TOTP not enabled")

            assertThatThrownBy { handler.handle(RegenerateRecoveryCodesCommand(code = "123456")) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("TOTP not enabled")

            verify(recoveryCodeRepository, never()).deleteByUserId(any())
        }
    }

    @Nested
    inner class InvalidCode {

        @Test
        fun `should throw when totp code is invalid`() {
            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(totpService.verifyCode(totpSecret, "999999")).thenReturn(false)
            `when`(messageService.getMessage("totp.recovery.invalid_code")).thenReturn("Invalid code")

            assertThatThrownBy { handler.handle(RegenerateRecoveryCodesCommand(code = "999999")) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("Invalid code")

            verify(recoveryCodeRepository, never()).deleteByUserId(any())
        }

        @Test
        fun `should throw when totp secret is null`() {
            user.totpSecret = null

            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(messageService.getMessage("totp.recovery.invalid_code")).thenReturn("Invalid code")

            assertThatThrownBy { handler.handle(RegenerateRecoveryCodesCommand(code = "123456")) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("Invalid code")
        }
    }
}
