package com.krachtix.identity.core.totp.command

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeEntity
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeRepository
import com.krachtix.identity.core.totp.service.TotpService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class RegenerateRecoveryCodesCommandHandler(
    private val userService: UserService,
    private val totpService: TotpService,
    private val recoveryCodeRepository: TotpRecoveryCodeRepository,
    private val messageService: MessageService
) : Command.Handler<RegenerateRecoveryCodesCommand, RegenerateRecoveryCodesResult> {

    override fun handle(command: RegenerateRecoveryCodesCommand): RegenerateRecoveryCodesResult {
        val user = userService.getCurrentUser()

        if (!user.totpEnabled) {
            throw InvalidRequestException(messageService.getMessage("totp.disable.not_enabled"))
        }

        val secret = user.totpSecret
            ?: throw InvalidRequestException(messageService.getMessage("totp.recovery.invalid_code"))

        if (!totpService.verifyCode(secret, command.code)) {
            throw InvalidRequestException(messageService.getMessage("totp.recovery.invalid_code"))
        }

        recoveryCodeRepository.deleteByUserId(user.id!!)

        val recoveryCodes = totpService.generateRecoveryCodes()
        recoveryCodes.forEach { code ->
            recoveryCodeRepository.save(
                TotpRecoveryCodeEntity(
                    user = user,
                    codeHash = totpService.hashRecoveryCode(code)
                )
            )
        }

        log.info { "Recovery codes regenerated for user ${user.username}" }

        return RegenerateRecoveryCodesResult(
            recoveryCodes = recoveryCodes,
            message = messageService.getMessage("totp.recovery.regenerated")
        )
    }
}
