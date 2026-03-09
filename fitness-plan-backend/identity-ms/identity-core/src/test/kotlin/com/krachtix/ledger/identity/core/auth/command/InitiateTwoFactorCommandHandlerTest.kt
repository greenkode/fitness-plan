package com.krachtix.identity.core.auth.command

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.TwoFactorAuthenticationRequiredException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.auth.service.JwtTokenService
import com.krachtix.identity.core.auth.service.TwoFactorEmailService
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.AccountLockoutService
import com.krachtix.identity.core.service.CustomUserDetails
import com.krachtix.identity.core.service.TokenGenerationUtility
import com.krachtix.identity.core.trusteddevice.domain.TrustedDevice
import com.krachtix.identity.core.trusteddevice.service.DeviceFingerprintService
import com.krachtix.identity.core.trusteddevice.service.TrustedDeviceService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import java.time.Duration
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InitiateTwoFactorCommandHandlerTest {

    @Mock
    private lateinit var authenticationManager: AuthenticationManager

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var accountLockoutService: AccountLockoutService

    @Mock
    private lateinit var twoFactorEmailService: TwoFactorEmailService

    @Mock
    private lateinit var tokenGenerationUtility: TokenGenerationUtility

    @Mock
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @Mock
    private lateinit var trustedDeviceService: TrustedDeviceService

    @Mock
    private lateinit var deviceFingerprintService: DeviceFingerprintService

    @Mock
    private lateinit var jwtTokenService: JwtTokenService

    @Mock
    private lateinit var refreshTokenService: RefreshTokenService

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var messageService: MessageService

    @Mock
    private lateinit var authentication: Authentication

    private lateinit var handler: InitiateTwoFactorCommandHandler

    private lateinit var user: OAuthUser
    private lateinit var userDetails: CustomUserDetails
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = InitiateTwoFactorCommandHandler(
            authenticationManager = authenticationManager,
            processGateway = processGateway,
            accountLockoutService = accountLockoutService,
            twoFactorEmailService = twoFactorEmailService,
            tokenGenerationUtility = tokenGenerationUtility,
            applicationEventPublisher = applicationEventPublisher,
            trustedDeviceService = trustedDeviceService,
            deviceFingerprintService = deviceFingerprintService,
            jwtTokenService = jwtTokenService,
            refreshTokenService = refreshTokenService,
            userRepository = userRepository,
            messageService = messageService,
            tokenLength = 6,
            skipForTrustedDevices = true,
            minIntervalSeconds = 60,
            maxCodesPerWindow = 5,
            windowSeconds = 1800,
            reverificationIntervalHours = 24
        )

        user = OAuthUser("testuser", "password").apply {
            id = userId
            email = Email("test@example.com")
            firstName = "Test"
            lastName = "User"
            emailVerified = true
            totpEnabled = true
            merchantId = UUID.randomUUID()
        }
        userDetails = CustomUserDetails(user)
    }

    @Nested
    inner class TrustedDeviceBypass {

        private val trustedDevice = TrustedDevice(
            id = UUID.randomUUID(),
            userId = userId,
            deviceFingerprint = "trusted-fp",
            deviceFingerprintHash = "hashed-fp",
            deviceName = "Test Device",
            expiresAt = Instant.now().plusSeconds(86400)
        )

        @Test
        fun `should bypass 2FA for trusted device within reverification window`() {
            user.twoFactorLastVerified = Instant.now().minus(Duration.ofHours(1))

            whenever(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>())).thenReturn(authentication)
            whenever(authentication.principal).thenReturn(userDetails)
            whenever(deviceFingerprintService.generateFingerprint(any(), any(), anyOrNull(), anyOrNull())).thenReturn("trusted-fp")
            whenever(trustedDeviceService.checkTrustedDevice(userId, "trusted-fp")).thenReturn(trustedDevice)
            whenever(jwtTokenService.generateToken(any(), any())).thenReturn("access-token")
            whenever(jwtTokenService.getTokenExpirySeconds()).thenReturn(600L)
            whenever(refreshTokenService.createRefreshToken(
                user = any(),
                ipAddress = any(),
                userAgent = any(),
                deviceFingerprint = any()
            )).thenReturn("refresh-token")

            val command = InitiateTwoFactorCommand(
                username = "testuser",
                password = "password".toCharArray(),
                ipAddress = "127.0.0.1",
                userAgent = "TestAgent"
            )

            val result = handler.handle(command)

            assertThat(result.accessToken).isEqualTo("access-token")
        }

        @Test
        fun `should require 2FA for trusted device when reverification interval exceeded`() {
            user.twoFactorLastVerified = Instant.now().minus(Duration.ofHours(25))

            whenever(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>())).thenReturn(authentication)
            whenever(authentication.principal).thenReturn(userDetails)
            whenever(deviceFingerprintService.generateFingerprint(any(), any(), anyOrNull(), anyOrNull())).thenReturn("trusted-fp")
            whenever(trustedDeviceService.checkTrustedDevice(userId, "trusted-fp")).thenReturn(trustedDevice)
            whenever(processGateway.createProcess(any())).thenReturn(mock())
            whenever(jwtTokenService.generateRestrictedTwoFactorToken(user)).thenReturn("restricted-token")
            whenever(messageService.getMessage("twofactor.totp.required")).thenReturn("TOTP required")

            val command = InitiateTwoFactorCommand(
                username = "testuser",
                password = "password".toCharArray(),
                ipAddress = "127.0.0.1",
                userAgent = "TestAgent"
            )

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(TwoFactorAuthenticationRequiredException::class.java)
                .extracting("twoFactorMethod")
                .isEqualTo("TOTP")
        }

        @Test
        fun `should require 2FA for trusted device when twoFactorLastVerified is null`() {
            user.twoFactorLastVerified = null

            whenever(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>())).thenReturn(authentication)
            whenever(authentication.principal).thenReturn(userDetails)
            whenever(deviceFingerprintService.generateFingerprint(any(), any(), anyOrNull(), anyOrNull())).thenReturn("trusted-fp")
            whenever(trustedDeviceService.checkTrustedDevice(userId, "trusted-fp")).thenReturn(trustedDevice)
            whenever(processGateway.createProcess(any())).thenReturn(mock())
            whenever(jwtTokenService.generateRestrictedTwoFactorToken(user)).thenReturn("restricted-token")
            whenever(messageService.getMessage("twofactor.totp.required")).thenReturn("TOTP required")

            val command = InitiateTwoFactorCommand(
                username = "testuser",
                password = "password".toCharArray(),
                ipAddress = "127.0.0.1",
                userAgent = "TestAgent"
            )

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(TwoFactorAuthenticationRequiredException::class.java)
                .extracting("twoFactorMethod")
                .isEqualTo("TOTP")
        }
    }

    @Nested
    inner class TotpRequired {

        @Test
        fun `should throw TwoFactorAuthenticationRequiredException for TOTP user without trusted device`() {
            whenever(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>())).thenReturn(authentication)
            whenever(authentication.principal).thenReturn(userDetails)
            whenever(deviceFingerprintService.generateFingerprint(any(), any(), anyOrNull(), anyOrNull())).thenReturn("unknown-fp")
            whenever(trustedDeviceService.checkTrustedDevice(userId, "unknown-fp")).thenReturn(null)
            whenever(processGateway.createProcess(any())).thenReturn(mock())
            whenever(jwtTokenService.generateRestrictedTwoFactorToken(user)).thenReturn("restricted-token")
            whenever(messageService.getMessage("twofactor.totp.required")).thenReturn("TOTP required")

            val command = InitiateTwoFactorCommand(
                username = "testuser",
                password = "password".toCharArray(),
                ipAddress = "127.0.0.1",
                userAgent = "TestAgent"
            )

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(TwoFactorAuthenticationRequiredException::class.java)
                .extracting("twoFactorMethod")
                .isEqualTo("TOTP")
        }
    }
}
