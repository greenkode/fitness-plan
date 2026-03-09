package com.krachtix.identity.core.totp.query

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetTotpStatusQueryHandler(
    private val userService: UserService,
    private val recoveryCodeRepository: TotpRecoveryCodeRepository
) : Command.Handler<GetTotpStatusQuery, GetTotpStatusResult> {

    override fun handle(command: GetTotpStatusQuery): GetTotpStatusResult {
        val user = userService.getCurrentUser()

        val remainingCodes = when {
            user.totpEnabled -> recoveryCodeRepository.countByUserIdAndUsedFalse(user.id!!)
            else -> 0L
        }

        log.info { "TOTP status retrieved for user ${user.username}: enabled=${user.totpEnabled}" }

        return GetTotpStatusResult(
            totpEnabled = user.totpEnabled,
            remainingRecoveryCodes = remainingCodes
        )
    }
}
