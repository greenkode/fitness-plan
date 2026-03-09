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
import com.krachtix.identity.core.totp.service.TotpService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class InitiateTotpSetupCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val totpService: TotpService,
    private val processGateway: ProcessGateway,
    private val messageService: MessageService
) : Command.Handler<InitiateTotpSetupCommand, InitiateTotpSetupResult> {

    override fun handle(command: InitiateTotpSetupCommand): InitiateTotpSetupResult {
        val user = userService.getCurrentUser()

        if (user.totpEnabled) {
            throw InvalidRequestException(messageService.getMessage("totp.setup.already_enabled"))
        }

        val secret = totpService.generateSecret()
        val qrCodeUri = totpService.generateQrCodeUri(secret, user.email?.value ?: user.username)

        user.totpSecret = secret
        userRepository.save(user)

        processGateway.createProcess(
            CreateNewProcessPayload(
                userId = user.id!!,
                publicId = UUID.randomUUID(),
                type = ProcessType.TOTP_SETUP,
                description = "TOTP authenticator setup for ${user.email?.value}",
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

        log.info { "TOTP setup initiated for user ${user.username}" }

        return InitiateTotpSetupResult(
            secret = secret,
            qrCodeUri = qrCodeUri,
            message = messageService.getMessage("totp.setup.initiated")
        )
    }
}
