package com.krachtix.identity.core.profile.command

import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.ProcessRequestDto
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.auth.service.UserTokenRevocationService
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.RegistrationSource
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ChangePasswordCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var messageService: MessageService

    @Mock
    private lateinit var refreshTokenService: RefreshTokenService

    @Mock
    private lateinit var userTokenRevocationService: UserTokenRevocationService

    @InjectMocks
    private lateinit var handler: ChangePasswordCommandHandler

    private lateinit var user: OAuthUser
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        user = OAuthUser("testuser", "encodedCurrentPassword").apply {
            id = userId
            registrationSource = RegistrationSource.SELF_REGISTRATION
        }
    }

    @Nested
    inner class WhenPasswordChangeSucceeds {

        @Test
        fun `should successfully change password`() {
            val command = ChangePasswordCommand(
                currentPassword = "currentPass".toCharArray(),
                newPassword = "newPass123".toCharArray()
            )

            val processDto = createProcessDto()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(passwordEncoder.matches("currentPass", "encodedCurrentPassword")).thenReturn(true)
            whenever(passwordEncoder.matches("newPass123", "encodedCurrentPassword")).thenReturn(false)
            whenever(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPassword")
            whenever(userRepository.save(user)).thenReturn(user)
            whenever(processGateway.findPendingProcessByPublicId(any())).thenReturn(processDto)
            whenever(messageService.getMessage("auth.password.change_success")).thenReturn("Password changed successfully")

            val result = handler.handle(command)

            assertThat(result.success).isTrue()
            assertThat(result.message).isEqualTo("Password changed successfully")
            verify(userRepository).save(user)
            assertThat(user.password).isEqualTo("encodedNewPassword")
            verify(refreshTokenService).revokeAllUserTokens(userId)
            verify(userTokenRevocationService).revokeUserAccessTokens(userId)
        }
    }

    @Nested
    inner class WhenCurrentPasswordIsIncorrect {

        @Test
        fun `should throw when current password is incorrect`() {
            val command = ChangePasswordCommand(
                currentPassword = "wrongPass".toCharArray(),
                newPassword = "newPass123".toCharArray()
            )

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(passwordEncoder.matches("wrongPass", "encodedCurrentPassword")).thenReturn(false)
            whenever(messageService.getMessage("auth.password.current_invalid")).thenReturn("Current password is invalid")

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(InvalidRequestException::class.java)

            verify(userRepository, never()).save(any())
            verify(refreshTokenService, never()).revokeAllUserTokens(any())
            verify(userTokenRevocationService, never()).revokeUserAccessTokens(any())
        }
    }

    @Nested
    inner class WhenNewPasswordSameAsCurrent {

        @Test
        fun `should throw when new password is same as current`() {
            val command = ChangePasswordCommand(
                currentPassword = "samePass".toCharArray(),
                newPassword = "samePass".toCharArray()
            )

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(passwordEncoder.matches("samePass", "encodedCurrentPassword")).thenReturn(true)
            whenever(messageService.getMessage("auth.password.same_as_current")).thenReturn("New password must be different")

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(InvalidRequestException::class.java)

            verify(userRepository, never()).save(any())
            verify(refreshTokenService, never()).revokeAllUserTokens(any())
            verify(userTokenRevocationService, never()).revokeUserAccessTokens(any())
        }
    }

    @Nested
    inner class WhenUserIsOAuth {

        @Test
        fun `should throw when user registered via Google OAuth`() {
            user.registrationSource = RegistrationSource.OAUTH_GOOGLE

            val command = ChangePasswordCommand(
                currentPassword = "currentPass".toCharArray(),
                newPassword = "newPass123".toCharArray()
            )

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(messageService.getMessage("auth.password.oauth_user_cannot_change"))
                .thenReturn("OAuth users cannot change password")

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(InvalidRequestException::class.java)

            verify(passwordEncoder, never()).matches(any(), any())
            verify(userRepository, never()).save(any())
            verify(refreshTokenService, never()).revokeAllUserTokens(any())
            verify(userTokenRevocationService, never()).revokeUserAccessTokens(any())
        }

        @Test
        fun `should throw when user registered via Microsoft OAuth`() {
            user.registrationSource = RegistrationSource.OAUTH_MICROSOFT

            val command = ChangePasswordCommand(
                currentPassword = "currentPass".toCharArray(),
                newPassword = "newPass123".toCharArray()
            )

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(messageService.getMessage("auth.password.oauth_user_cannot_change"))
                .thenReturn("OAuth users cannot change password")

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(InvalidRequestException::class.java)

            verify(passwordEncoder, never()).matches(any(), any())
            verify(userRepository, never()).save(any())
            verify(refreshTokenService, never()).revokeAllUserTokens(any())
            verify(userTokenRevocationService, never()).revokeUserAccessTokens(any())
        }
    }

    @Nested
    inner class TokenRevocation {

        @Test
        fun `should revoke all refresh tokens and access tokens on successful password change`() {
            val command = ChangePasswordCommand(
                currentPassword = "currentPass".toCharArray(),
                newPassword = "newPass123".toCharArray()
            )

            val processDto = createProcessDto()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(passwordEncoder.matches("currentPass", "encodedCurrentPassword")).thenReturn(true)
            whenever(passwordEncoder.matches("newPass123", "encodedCurrentPassword")).thenReturn(false)
            whenever(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPassword")
            whenever(userRepository.save(user)).thenReturn(user)
            whenever(processGateway.findPendingProcessByPublicId(any())).thenReturn(processDto)
            whenever(messageService.getMessage("auth.password.change_success")).thenReturn("Password changed successfully")

            handler.handle(command)

            verify(refreshTokenService).revokeAllUserTokens(userId)
            verify(userTokenRevocationService).revokeUserAccessTokens(userId)
        }
    }

    @Nested
    inner class PasswordCharArrayZeroing {

        @Test
        fun `should zero password char arrays after successful password change`() {
            val currentPasswordChars = "currentPass".toCharArray()
            val newPasswordChars = "newPass123".toCharArray()
            val command = ChangePasswordCommand(
                currentPassword = currentPasswordChars,
                newPassword = newPasswordChars
            )

            val processDto = createProcessDto()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(passwordEncoder.matches("currentPass", "encodedCurrentPassword")).thenReturn(true)
            whenever(passwordEncoder.matches("newPass123", "encodedCurrentPassword")).thenReturn(false)
            whenever(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPassword")
            whenever(userRepository.save(user)).thenReturn(user)
            whenever(processGateway.findPendingProcessByPublicId(any())).thenReturn(processDto)
            whenever(messageService.getMessage("auth.password.change_success")).thenReturn("Password changed successfully")

            handler.handle(command)

            assertThat(command.currentPassword).containsOnly('\u0000')
            assertThat(command.newPassword).containsOnly('\u0000')
        }

        @Test
        fun `should zero password char arrays even when exception is thrown`() {
            val currentPasswordChars = "wrongPass".toCharArray()
            val newPasswordChars = "newPass123".toCharArray()
            val command = ChangePasswordCommand(
                currentPassword = currentPasswordChars,
                newPassword = newPasswordChars
            )

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(passwordEncoder.matches("wrongPass", "encodedCurrentPassword")).thenReturn(false)
            whenever(messageService.getMessage("auth.password.current_invalid")).thenReturn("Current password is invalid")

            runCatching { handler.handle(command) }

            assertThat(command.currentPassword).containsOnly('\u0000')
            assertThat(command.newPassword).containsOnly('\u0000')
        }

        @Test
        fun `should zero password char arrays when OAuth user attempts password change`() {
            user.registrationSource = RegistrationSource.OAUTH_GOOGLE

            val currentPasswordChars = "currentPass".toCharArray()
            val newPasswordChars = "newPass123".toCharArray()
            val command = ChangePasswordCommand(
                currentPassword = currentPasswordChars,
                newPassword = newPasswordChars
            )

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(messageService.getMessage("auth.password.oauth_user_cannot_change"))
                .thenReturn("OAuth users cannot change password")

            runCatching { handler.handle(command) }

            assertThat(command.currentPassword).containsOnly('\u0000')
            assertThat(command.newPassword).containsOnly('\u0000')
        }
    }

    private fun createProcessDto(): ProcessDto {
        val requestDto = ProcessRequestDto(
            id = 1L,
            type = ProcessRequestType.CREATE_NEW_PROCESS,
            state = ProcessState.COMPLETE,
            stakeholders = emptyMap(),
            data = emptyMap()
        )

        return ProcessDto(
            id = 1L,
            publicId = UUID.randomUUID(),
            state = ProcessState.PENDING,
            type = ProcessType.PASSWORD_CHANGE,
            channel = ProcessChannel.BUSINESS_WEB,
            createdDate = Instant.now(),
            requests = listOf(requestDto)
        )
    }
}
