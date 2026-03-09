package com.krachtix.identity.core.auth.command

import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.auth.service.UserTokenRevocationService
import com.krachtix.identity.core.refreshtoken.domain.RefreshTokenEntity
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class LogoutCommandHandlerTest {

    @Mock
    private lateinit var refreshTokenService: RefreshTokenService

    @Mock
    private lateinit var messageService: MessageService

    @Mock
    private lateinit var userTokenRevocationService: UserTokenRevocationService

    @InjectMocks
    private lateinit var handler: LogoutCommandHandler

    private val userId = UUID.randomUUID()

    @Nested
    inner class WhenLogoutSucceeds {

        @Test
        fun `should revoke refresh token and access tokens on successful logout`() {
            val refreshTokenValue = "valid-refresh-token"
            val command = LogoutCommand(refreshToken = refreshTokenValue)

            val refreshTokenEntity = RefreshTokenEntity(
                jti = UUID.randomUUID().toString(),
                userId = userId,
                tokenHash = "hashed-token",
                issuedAt = Instant.now(),
                expiresAt = Instant.now().plusSeconds(86400)
            )

            whenever(refreshTokenService.findRefreshTokenByHash(refreshTokenValue)).thenReturn(refreshTokenEntity)
            whenever(messageService.getMessage("auth.success.logout")).thenReturn("Logged out successfully")

            val result = handler.handle(command)

            assertThat(result.success).isTrue()
            assertThat(result.message).isEqualTo("Logged out successfully")
            verify(refreshTokenService).revokeRefreshTokenByHash(refreshTokenValue)
            verify(userTokenRevocationService).revokeUserAccessTokens(userId)
        }
    }

    @Nested
    inner class WhenRefreshTokenNotFound {

        @Test
        fun `should return success and skip access token revocation when refresh token not found`() {
            val refreshTokenValue = "unknown-refresh-token"
            val command = LogoutCommand(refreshToken = refreshTokenValue)

            whenever(refreshTokenService.findRefreshTokenByHash(refreshTokenValue)).thenReturn(null)
            whenever(messageService.getMessage("auth.success.logout")).thenReturn("Logged out successfully")

            val result = handler.handle(command)

            assertThat(result.success).isTrue()
            assertThat(result.message).isEqualTo("Logged out successfully")
            verify(refreshTokenService).revokeRefreshTokenByHash(refreshTokenValue)
            verify(userTokenRevocationService, never()).revokeUserAccessTokens(userId)
        }
    }

    @Nested
    inner class WhenRevocationFails {

        @Test
        fun `should return success even when revocation throws exception`() {
            val refreshTokenValue = "error-refresh-token"
            val command = LogoutCommand(refreshToken = refreshTokenValue)

            whenever(refreshTokenService.findRefreshTokenByHash(refreshTokenValue))
                .thenThrow(RuntimeException("Database error"))
            whenever(messageService.getMessage("auth.success.logout")).thenReturn("Logged out successfully")

            val result = handler.handle(command)

            assertThat(result.success).isTrue()
            assertThat(result.message).isEqualTo("Logged out successfully")
        }
    }
}
