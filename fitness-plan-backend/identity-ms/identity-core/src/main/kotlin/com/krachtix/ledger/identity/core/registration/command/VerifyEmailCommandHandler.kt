package com.krachtix.identity.core.registration.command

import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.MakeProcessRequestPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class VerifyEmailCommandHandler(
    private val processGateway: ProcessGateway,
    private val userRepository: OAuthUserRepository,
    private val messageService: MessageService
) : Command.Handler<VerifyEmailCommand, VerifyEmailResult> {

    override fun handle(command: VerifyEmailCommand): VerifyEmailResult {
        log.info { "Verifying email with token: ${command.token}" }

        val process = processGateway.findPendingProcessByTypeAndExternalReference(
            type = ProcessType.EMAIL_VERIFICATION,
            externalReference = command.token
        ) ?: throw InvalidRequestException(messageService.getMessage("registration.error.invalid_verification_token"))

        val initialRequest = process.getInitialRequest()
        val userId = initialRequest.getDataValueOrNull(ProcessRequestDataName.USER_IDENTIFIER)
            ?.let { UUID.fromString(it) }
            ?: throw InvalidRequestException(messageService.getMessage("registration.error.user_not_found"))

        val user = userRepository.findById(userId)
            .orElseThrow { RecordNotFoundException(messageService.getMessage("registration.error.user_not_found")) }

        user.emailVerified = true
        user.registrationComplete = true
        userRepository.save(user)

        val completeRequest = MakeProcessRequestPayload(
            userId = userId,
            publicId = process.publicId,
            eventType = ProcessEvent.PROCESS_COMPLETED,
            requestType = ProcessRequestType.COMPLETE_PROCESS,
            channel = ProcessChannel.BUSINESS_WEB
        )

        processGateway.makeRequest(completeRequest)

        log.info { "Email verified successfully for user: ${user.id}" }

        return VerifyEmailResult(
            success = true,
            message = messageService.getMessage("registration.success.email_verified"),
            userId = user.id
        )
    }
}
