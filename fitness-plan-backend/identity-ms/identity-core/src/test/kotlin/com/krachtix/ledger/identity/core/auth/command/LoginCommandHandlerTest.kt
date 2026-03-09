package com.krachtix.identity.core.auth.command

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.InvalidCredentialsException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.auth.service.JwtTokenService
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import com.krachtix.identity.core.service.AccountLockedException
import com.krachtix.identity.core.service.AccountLockoutService
import com.krachtix.identity.core.service.CustomUserDetails
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class LoginCommandHandlerTest {

    @Mock
    private lateinit var jwtTokenService: JwtTokenService

    @Mock
    private lateinit var refreshTokenService: RefreshTokenService

    @Mock
    private lateinit var authenticationManager: AuthenticationManager

    @Mock
    private lateinit var accountLockoutService: AccountLockoutService

    @Mock
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @Mock
    private lateinit var messageService: MessageService

    @Mock
    private lateinit var authentication: Authentication

    @InjectMocks
    private lateinit var handler: LoginCommandHandler

    private lateinit var oauthUser: OAuthUser
    private lateinit var userDetails: CustomUserDetails
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        oauthUser = OAuthUser("testuser", "password").apply {
            id = userId
            email = Email("test@example.com")
            firstName = "Test"
            lastName = "User"
        }
        userDetails = CustomUserDetails(oauthUser)
    }

    @Nested
    inner class WhenLoginSucceeds {

        @Test
        fun `should return login result with token and user details`() {
            val command = LoginCommand(
                username = "testuser",
                password = "correctPassword".toCharArray(),
                ipAddress = "127.0.0.1"
            )

            whenever(authenticationManager.authenticate(any())).thenReturn(authentication)
            whenever(authentication.principal).thenReturn(userDetails)
            whenever(jwtTokenService.generateToken(oauthUser, userDetails)).thenReturn("access-token-123")
            whenever(jwtTokenService.getTokenExpirySeconds()).thenReturn(3600L)
            whenever(refreshTokenService.createRefreshToken(
                user = oauthUser,
                ipAddress = "127.0.0.1",
                userAgent = null,
                deviceFingerprint = null
            )).thenReturn("refresh-token-456")

            val result = handler.handle(command)

            assertThat(result.accessToken).isEqualTo("access-token-123")
            assertThat(result.refreshToken).isEqualTo("refresh-token-456")
            assertThat(result.expiresIn).isEqualTo(3600L)
            assertThat(result.tokenType).isEqualTo("Bearer")
            assertThat(result.username).isEqualTo("testuser")
            assertThat(result.fullName).isEqualTo("Test User")
            assertThat(result.email).isEqualTo("test@example.com")
            verify(accountLockoutService).handleSuccessfulLogin("testuser")
            verify(applicationEventPublisher).publishEvent(any<Any>())
        }
    }

    @Nested
    inner class WhenCredentialsAreInvalid {

        @Test
        fun `should throw InvalidCredentialsException on bad credentials`() {
            val command = LoginCommand(
                username = "testuser",
                password = "wrongPassword".toCharArray()
            )

            whenever(authenticationManager.authenticate(any()))
                .thenThrow(BadCredentialsException("Bad credentials"))
            whenever(messageService.getMessage("auth.error.invalid_credentials"))
                .thenReturn("Invalid credentials")

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(InvalidCredentialsException::class.java)

            verify(accountLockoutService).handleFailedLogin("testuser")
        }
    }

    @Nested
    inner class WhenAccountIsLocked {

        @Test
        fun `should rethrow AccountLockedException`() {
            val lockedUntil = Instant.now().plusSeconds(1800)
            val command = LoginCommand(
                username = "testuser",
                password = "anyPassword".toCharArray()
            )

            whenever(authenticationManager.authenticate(any()))
                .thenThrow(AccountLockedException("testuser", lockedUntil, 5))

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(AccountLockedException::class.java)

            verify(applicationEventPublisher).publishEvent(any<Any>())
        }
    }

    @Nested
    inner class PasswordCharArrayZeroing {

        @Test
        fun `should zero password char array after successful login`() {
            val passwordChars = "correctPassword".toCharArray()
            val command = LoginCommand(
                username = "testuser",
                password = passwordChars,
                ipAddress = "127.0.0.1"
            )

            whenever(authenticationManager.authenticate(any())).thenReturn(authentication)
            whenever(authentication.principal).thenReturn(userDetails)
            whenever(jwtTokenService.generateToken(oauthUser, userDetails)).thenReturn("access-token-123")
            whenever(jwtTokenService.getTokenExpirySeconds()).thenReturn(3600L)
            whenever(refreshTokenService.createRefreshToken(
                user = oauthUser,
                ipAddress = "127.0.0.1",
                userAgent = null,
                deviceFingerprint = null
            )).thenReturn("refresh-token-456")

            handler.handle(command)

            assertThat(command.password).containsOnly('\u0000')
        }

        @Test
        fun `should zero password char array after failed login`() {
            val passwordChars = "wrongPassword".toCharArray()
            val command = LoginCommand(
                username = "testuser",
                password = passwordChars
            )

            whenever(authenticationManager.authenticate(any()))
                .thenThrow(BadCredentialsException("Bad credentials"))
            whenever(messageService.getMessage("auth.error.invalid_credentials"))
                .thenReturn("Invalid credentials")

            runCatching { handler.handle(command) }

            assertThat(command.password).containsOnly('\u0000')
        }
    }
}
