package com.krachtix.identity.core.totp.command

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeEntity
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeRepository
import com.krachtix.identity.core.totp.service.TotpService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class ConfirmTotpSetupCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val totpService: TotpService,
    private val recoveryCodeRepository: TotpRecoveryCodeRepository,
    private val processGateway: ProcessGateway,
    private val messageService: MessageService
) : Command.Handler<ConfirmTotpSetupCommand, ConfirmTotpSetupResult> {

    override fun handle(command: ConfirmTotpSetupCommand): ConfirmTotpSetupResult {
        val user = userService.getCurrentUser()

        if (user.totpEnabled) {
            throw InvalidRequestException(messageService.getMessage("totp.setup.already_enabled"))
        }

        val secret = user.totpSecret
            ?: throw InvalidRequestException(messageService.getMessage("totp.setup.invalid_code"))

        if (!totpService.verifyCode(secret, command.code)) {
            throw InvalidRequestException(messageService.getMessage("totp.setup.invalid_code"))
        }

        user.totpEnabled = true
        user.twoFactorLastVerified = java.time.Instant.now()
        userRepository.save(user)

        val recoveryCodes = totpService.generateRecoveryCodes()
        recoveryCodes.forEach { code ->
            recoveryCodeRepository.save(
                TotpRecoveryCodeEntity(
                    user = user,
                    codeHash = totpService.hashRecoveryCode(code)
                )
            )
        }

        val pendingProcess = processGateway.findRecentPendingProcessesByTypeAndForUserId(
            ProcessType.TOTP_SETUP, user.id!!, java.time.Instant.now().minusSeconds(600)
        ).maxByOrNull { it.createdDate }

        pendingProcess?.let { processGateway.completeProcess(it.publicId, it.getInitialRequest().id) }

        log.info { "TOTP setup confirmed for user ${user.username}" }

        return ConfirmTotpSetupResult(
            recoveryCodes = recoveryCodes,
            message = messageService.getMessage("totp.setup.confirmed")
        )
    }
}
