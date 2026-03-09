package com.krachtix.identity.core.auth.command

import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.auth.service.UserTokenRevocationService
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class LogoutCommandHandler(
    private val refreshTokenService: RefreshTokenService,
    private val messageService: MessageService,
    private val userTokenRevocationService: UserTokenRevocationService
) : Command.Handler<LogoutCommand, LogoutResult> {

    override fun handle(command: LogoutCommand): LogoutResult {
        log.info { "Logout request received" }

        return runCatching {
            val refreshToken = refreshTokenService.findRefreshTokenByHash(command.refreshToken)
            refreshTokenService.revokeRefreshTokenByHash(command.refreshToken)
            refreshToken?.let { userTokenRevocationService.revokeUserAccessTokens(it.userId) }
            log.info { "Logout successful - refresh token and access tokens revoked" }

            LogoutResult(
                success = true,
                message = messageService.getMessage("auth.success.logout")
            )
        }.getOrElse { e ->
            log.warn(e) { "Error during logout" }
            LogoutResult(
                success = true,
                message = messageService.getMessage("auth.success.logout")
            )
        }
    }
}
