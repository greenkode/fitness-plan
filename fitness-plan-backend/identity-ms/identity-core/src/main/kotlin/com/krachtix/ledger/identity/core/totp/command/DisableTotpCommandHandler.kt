package com.krachtix.identity.core.totp.command

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeRepository
import com.krachtix.identity.core.totp.service.TotpService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class DisableTotpCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val totpService: TotpService,
    private val recoveryCodeRepository: TotpRecoveryCodeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val processGateway: ProcessGateway,
    private val messageService: MessageService
) : Command.Handler<DisableTotpCommand, DisableTotpResult> {

    override fun handle(command: DisableTotpCommand): DisableTotpResult {
        val user = userService.getCurrentUser()

        if (!user.totpEnabled) {
            throw InvalidRequestException(messageService.getMessage("totp.disable.not_enabled"))
        }

        val verified = when {
            command.code != null -> {
                val secret = user.totpSecret
                    ?: throw InvalidRequestException(messageService.getMessage("totp.disable.invalid_verification"))
                totpService.verifyCode(secret, command.code)
            }
            command.password != null -> passwordEncoder.matches(command.password, user.password)
            else -> throw InvalidRequestException(messageService.getMessage("totp.disable.invalid_verification"))
        }

        if (!verified) {
            throw InvalidRequestException(messageService.getMessage("totp.disable.invalid_verification"))
        }

        val processId = UUID.randomUUID()
        processGateway.createProcess(
            CreateNewProcessPayload(
                userId = user.id!!,
                publicId = processId,
                type = ProcessType.TOTP_DISABLE,
                description = "TOTP authenticator disabled for ${user.email?.value}",
                initialState = ProcessState.PENDING,
                requestState = ProcessState.COMPLETE,
                channel = ProcessChannel.BUSINESS_WEB,
                data = mapOf(
                    ProcessRequestDataName.USER_IDENTIFIER to user.id.toString()
                ),
                stakeholders = mapOf(
                    ProcessStakeholderType.FOR_USER to user.id.toString()
                )
            )
        )

        user.totpSecret = null
        user.totpEnabled = false
        userRepository.save(user)

        recoveryCodeRepository.deleteByUserId(user.id!!)

        processGateway.findPendingProcessByPublicId(processId)?.let {
            processGateway.completeProcess(it.publicId, it.getInitialRequest().id)
        }

        log.info { "TOTP disabled for user ${user.username}" }

        return DisableTotpResult(
            message = messageService.getMessage("totp.disable.success")
        )
    }
}
