package com.krachtix.identity.core.auth.command

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.TwoFactorAuthenticationRequiredException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.ProcessRequestDto
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.auth.service.JwtTokenService
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.refreshtoken.domain.RefreshTokenEntity
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import com.krachtix.identity.core.repository.OAuthUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RefreshTokenCommandHandlerTest {

    @Mock
    private lateinit var jwtTokenService: JwtTokenService

    @Mock
    private lateinit var refreshTokenService: RefreshTokenService

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @Mock
    private lateinit var messageService: MessageService

    @Mock
    private lateinit var processGateway: ProcessGateway

    private lateinit var handler: RefreshTokenCommandHandler

    private lateinit var user: OAuthUser
    private lateinit var storedToken: RefreshTokenEntity
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = RefreshTokenCommandHandler(
            jwtTokenService = jwtTokenService,
            refreshTokenService = refreshTokenService,
            userRepository = userRepository,
            applicationEventPublisher = applicationEventPublisher,
            messageService = messageService,
            processGateway = processGateway,
            reverificationIntervalHours = 24
        )

        user = OAuthUser("testuser", "password").apply {
            id = userId
            email = Email("test@example.com")
            firstName = "Test"
            lastName = "User"
            emailVerified = true
            merchantId = UUID.randomUUID()
        }

        storedToken = RefreshTokenEntity(
            jti = "test-jti",
            userId = userId,
            tokenHash = "hash",
            expiresAt = Instant.now().plusSeconds(86400)
        )
    }

    @Nested
    inner class WhenTwoFactorNotEnabled {

        @Test
        fun `should refresh tokens when 2FA not enabled`() {
            user.totpEnabled = false

            whenever(refreshTokenService.validateAndGetRefreshTokenByHash("refresh-token")).thenReturn(storedToken)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(jwtTokenService.generateToken(any(), any())).thenReturn("new-access-token")
            whenever(refreshTokenService.rotateRefreshToken(storedToken, user)).thenReturn("new-refresh-token")
            whenever(jwtTokenService.getTokenExpirySeconds()).thenReturn(600L)

            val result = handler.handle(RefreshTokenCommand("refresh-token"))

            assertThat(result.accessToken).isEqualTo("new-access-token")
            assertThat(result.refreshToken).isEqualTo("new-refresh-token")
            assertThat(result.expiresIn).isEqualTo(600L)
            verify(processGateway, never()).createProcess(any())
        }
    }

    @Nested
    inner class WhenTwoFactorEnabledAndRecent {

        @Test
        fun `should refresh tokens when twoFactorLastVerified is recent`() {
            user.totpEnabled = true
            user.twoFactorLastVerified = Instant.now().minusSeconds(3600)

            whenever(refreshTokenService.validateAndGetRefreshTokenByHash("refresh-token")).thenReturn(storedToken)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(jwtTokenService.generateToken(any(), any())).thenReturn("new-access-token")
            whenever(refreshTokenService.rotateRefreshToken(storedToken, user)).thenReturn("new-refresh-token")
            whenever(jwtTokenService.getTokenExpirySeconds()).thenReturn(600L)

            val result = handler.handle(RefreshTokenCommand("refresh-token"))

            assertThat(result.accessToken).isEqualTo("new-access-token")
            assertThat(result.refreshToken).isEqualTo("new-refresh-token")
            verify(processGateway, never()).createProcess(any())
        }
    }

    @Nested
    inner class WhenTwoFactorLastVerifiedIsNull {

        @Test
        fun `should throw TwoFactorAuthenticationRequiredException when twoFactorLastVerified is null`() {
            user.totpEnabled = true
            user.twoFactorLastVerified = null

            val processDto = ProcessDto(
                id = 1L,
                publicId = UUID.randomUUID(),
                state = ProcessState.PENDING,
                type = ProcessType.TWO_FACTOR_AUTH,
                channel = ProcessChannel.BUSINESS_WEB,
                createdDate = Instant.now(),
                requests = listOf(
                    ProcessRequestDto(
                        id = 1L,
                        type = ProcessRequestType.CREATE_NEW_PROCESS,
                        state = ProcessState.COMPLETE,
                        stakeholders = emptyMap(),
                        data = emptyMap()
                    )
                )
            )

            whenever(refreshTokenService.validateAndGetRefreshTokenByHash("refresh-token")).thenReturn(storedToken)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(processGateway.createProcess(any())).thenReturn(processDto)
            whenever(jwtTokenService.generateRestrictedTwoFactorToken(user)).thenReturn("restricted-token")
            whenever(messageService.getMessage("twofactor.reverification.required")).thenReturn("Please re-verify")

            assertThatThrownBy { handler.handle(RefreshTokenCommand("refresh-token")) }
                .isInstanceOf(TwoFactorAuthenticationRequiredException::class.java)
                .hasMessage("Please re-verify")

            verify(processGateway).createProcess(any())
            verify(jwtTokenService).generateRestrictedTwoFactorToken(user)
            verify(jwtTokenService, never()).generateToken(any(), any())
        }
    }

    @Nested
    inner class WhenTwoFactorLastVerifiedIsStale {

        @Test
        fun `should throw TwoFactorAuthenticationRequiredException when twoFactorLastVerified is stale`() {
            user.totpEnabled = true
            user.twoFactorLastVerified = Instant.now().minusSeconds(25 * 3600)

            val processDto = ProcessDto(
                id = 1L,
                publicId = UUID.randomUUID(),
                state = ProcessState.PENDING,
                type = ProcessType.TWO_FACTOR_AUTH,
                channel = ProcessChannel.BUSINESS_WEB,
                createdDate = Instant.now(),
                requests = listOf(
                    ProcessRequestDto(
                        id = 1L,
                        type = ProcessRequestType.CREATE_NEW_PROCESS,
                        state = ProcessState.COMPLETE,
                        stakeholders = emptyMap(),
                        data = emptyMap()
                    )
                )
            )

            whenever(refreshTokenService.validateAndGetRefreshTokenByHash("refresh-token")).thenReturn(storedToken)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(processGateway.createProcess(any())).thenReturn(processDto)
            whenever(jwtTokenService.generateRestrictedTwoFactorToken(user)).thenReturn("restricted-token")
            whenever(messageService.getMessage("twofactor.reverification.required")).thenReturn("Please re-verify")

            assertThatThrownBy { handler.handle(RefreshTokenCommand("refresh-token")) }
                .isInstanceOf(TwoFactorAuthenticationRequiredException::class.java)
                .extracting("twoFactorMethod")
                .isEqualTo("TOTP")

            verify(processGateway).createProcess(any())
        }
    }

    @Nested
    inner class WhenEmailNotVerified {

        @Test
        fun `should throw InvalidRequestException when email not verified`() {
            user.emailVerified = false

            whenever(refreshTokenService.validateAndGetRefreshTokenByHash("refresh-token")).thenReturn(storedToken)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(messageService.getMessage("auth.error.email_not_verified")).thenReturn("Email not verified")

            assertThatThrownBy { handler.handle(RefreshTokenCommand("refresh-token")) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("Email not verified")

            verify(processGateway, never()).createProcess(any())
            verify(jwtTokenService, never()).generateToken(any(), any())
        }
    }
}
