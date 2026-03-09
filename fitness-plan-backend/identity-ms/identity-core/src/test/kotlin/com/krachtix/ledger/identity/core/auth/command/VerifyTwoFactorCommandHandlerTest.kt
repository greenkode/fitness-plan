package com.krachtix.identity.core.auth.command

import an.awesome.pipelinr.Pipeline
import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.TwoFactorCodeInvalidException
import com.krachtix.commons.exception.TwoFactorSessionInvalidException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.ProcessRequestDto
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.auth.service.JwtTokenService
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.AccountLockoutService
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeRepository
import com.krachtix.identity.core.totp.service.TotpService
import com.krachtix.identity.core.trusteddevice.service.DeviceFingerprintService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class VerifyTwoFactorCommandHandlerTest {

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var jwtTokenService: JwtTokenService

    @Mock
    private lateinit var refreshTokenService: RefreshTokenService

    @Mock
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @Mock
    private lateinit var pipeline: Pipeline

    @Mock
    private lateinit var deviceFingerprintService: DeviceFingerprintService

    @Mock
    private lateinit var totpService: TotpService

    @Mock
    private lateinit var recoveryCodeRepository: TotpRecoveryCodeRepository

    @Mock
    private lateinit var accountLockoutService: AccountLockoutService

    @Mock
    private lateinit var messageService: MessageService

    private lateinit var handler: VerifyTwoFactorCommandHandler

    private lateinit var user: OAuthUser
    private val userId = UUID.randomUUID()
    private val sessionId = "test-session-id"
    private val totpSecret = "JBSWY3DPEHPK3PXP"

    @BeforeEach
    fun setUp() {
        handler = VerifyTwoFactorCommandHandler(
            processGateway = processGateway,
            userRepository = userRepository,
            jwtTokenService = jwtTokenService,
            refreshTokenService = refreshTokenService,
            applicationEventPublisher = applicationEventPublisher,
            pipeline = pipeline,
            deviceFingerprintService = deviceFingerprintService,
            totpService = totpService,
            recoveryCodeRepository = recoveryCodeRepository,
            accountLockoutService = accountLockoutService,
            messageService = messageService,
            maxAttempts = 3,
            trustedDeviceEnabled = false,
            defaultTrustDurationDays = 30
        )

        user = OAuthUser("testuser", "password").apply {
            id = userId
            email = Email("test@example.com")
            firstName = "Test"
            lastName = "User"
            totpEnabled = true
            totpSecret = this@VerifyTwoFactorCommandHandlerTest.totpSecret
            merchantId = UUID.randomUUID()
        }
    }

    @Nested
    inner class SuccessfulVerification {

        @Test
        fun `should set twoFactorLastVerified on successful TOTP verification`() {
            val processPublicId = UUID.randomUUID()
            val initialRequest = ProcessRequestDto(
                id = 1L,
                type = ProcessRequestType.CREATE_NEW_PROCESS,
                state = ProcessState.COMPLETE,
                stakeholders = emptyMap(),
                data = mapOf(
                    ProcessRequestDataName.USER_IDENTIFIER to userId.toString(),
                    ProcessRequestDataName.TWO_FACTOR_METHOD to "TOTP"
                )
            )
            val processDto = ProcessDto(
                id = 1L,
                publicId = processPublicId,
                state = ProcessState.PENDING,
                type = ProcessType.TWO_FACTOR_AUTH,
                channel = ProcessChannel.BUSINESS_WEB,
                createdDate = Instant.now(),
                requests = listOf(initialRequest)
            )

            whenever(processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.TWO_FACTOR_AUTH, sessionId))
                .thenReturn(processDto)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            whenever(jwtTokenService.generateToken(any(), any())).thenReturn("access-token")
            whenever(jwtTokenService.getTokenExpirySeconds()).thenReturn(600L)
            whenever(refreshTokenService.createRefreshToken(
                user = any(),
                ipAddress = any(),
                userAgent = any(),
                deviceFingerprint = any()
            )).thenReturn("refresh-token")

            val command = VerifyTwoFactorCommand(
                sessionId = sessionId,
                code = "123456",
                ipAddress = "127.0.0.1",
                userAgent = "TestAgent"
            )

            handler.handle(command)

            assertThat(user.twoFactorLastVerified).isNotNull()
        }

        @Test
        fun `should return tokens on successful verification`() {
            val processPublicId = UUID.randomUUID()
            val initialRequest = ProcessRequestDto(
                id = 1L,
                type = ProcessRequestType.CREATE_NEW_PROCESS,
                state = ProcessState.COMPLETE,
                stakeholders = emptyMap(),
                data = mapOf(
                    ProcessRequestDataName.USER_IDENTIFIER to userId.toString(),
                    ProcessRequestDataName.TWO_FACTOR_METHOD to "TOTP"
                )
            )
            val processDto = ProcessDto(
                id = 1L,
                publicId = processPublicId,
                state = ProcessState.PENDING,
                type = ProcessType.TWO_FACTOR_AUTH,
                channel = ProcessChannel.BUSINESS_WEB,
                createdDate = Instant.now(),
                requests = listOf(initialRequest)
            )

            whenever(processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.TWO_FACTOR_AUTH, sessionId))
                .thenReturn(processDto)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(totpService.verifyCode(totpSecret, "123456")).thenReturn(true)
            whenever(jwtTokenService.generateToken(any(), any())).thenReturn("access-token")
            whenever(jwtTokenService.getTokenExpirySeconds()).thenReturn(600L)
            whenever(refreshTokenService.createRefreshToken(
                user = any(),
                ipAddress = any(),
                userAgent = any(),
                deviceFingerprint = any()
            )).thenReturn("refresh-token")

            val command = VerifyTwoFactorCommand(
                sessionId = sessionId,
                code = "123456",
                ipAddress = "127.0.0.1",
                userAgent = "TestAgent"
            )

            val result = handler.handle(command)

            assertThat(result.accessToken).isEqualTo("access-token")
            assertThat(result.refreshToken).isEqualTo("refresh-token")
        }
    }

    @Nested
    inner class InvalidSession {

        @Test
        fun `should throw when session is invalid`() {
            whenever(processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.TWO_FACTOR_AUTH, sessionId))
                .thenReturn(null)

            val command = VerifyTwoFactorCommand(
                sessionId = sessionId,
                code = "123456",
                ipAddress = "127.0.0.1",
                userAgent = "TestAgent"
            )

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(TwoFactorSessionInvalidException::class.java)
        }
    }

    @Nested
    inner class InvalidCode {

        @Test
        fun `should throw when TOTP code is invalid`() {
            val processPublicId = UUID.randomUUID()
            val initialRequest = ProcessRequestDto(
                id = 1L,
                type = ProcessRequestType.CREATE_NEW_PROCESS,
                state = ProcessState.COMPLETE,
                stakeholders = emptyMap(),
                data = mapOf(
                    ProcessRequestDataName.USER_IDENTIFIER to userId.toString(),
                    ProcessRequestDataName.TWO_FACTOR_METHOD to "TOTP"
                )
            )
            val processDto = ProcessDto(
                id = 1L,
                publicId = processPublicId,
                state = ProcessState.PENDING,
                type = ProcessType.TWO_FACTOR_AUTH,
                channel = ProcessChannel.BUSINESS_WEB,
                createdDate = Instant.now(),
                requests = listOf(initialRequest)
            )

            whenever(processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.TWO_FACTOR_AUTH, sessionId))
                .thenReturn(processDto)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(totpService.verifyCode(totpSecret, "999999")).thenReturn(false)
            whenever(recoveryCodeRepository.findByUserIdAndUsedFalse(userId)).thenReturn(emptyList())

            val command = VerifyTwoFactorCommand(
                sessionId = sessionId,
                code = "999999",
                ipAddress = "127.0.0.1",
                userAgent = "TestAgent"
            )

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(TwoFactorCodeInvalidException::class.java)

            assertThat(user.twoFactorLastVerified).isNull()
        }
    }
}
