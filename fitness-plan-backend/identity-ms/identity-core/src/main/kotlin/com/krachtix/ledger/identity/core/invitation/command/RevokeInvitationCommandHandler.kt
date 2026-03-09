package com.krachtix.identity.core.invitation.command

import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.audit.AuditEvent
import com.krachtix.identity.commons.audit.AuditResource
import com.krachtix.identity.commons.audit.IdentityType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.deletion.service.UserDeletionService
import com.krachtix.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class RevokeInvitationCommandHandler(
    private val userRepository: OAuthUserRepository,
    private val processGateway: ProcessGateway,
    private val userDeletionService: UserDeletionService,
    private val messageService: MessageService,
    private val applicationEventPublisher: ApplicationEventPublisher
) : Command.Handler<RevokeInvitationCommand, RevokeInvitationResult> {

    override fun handle(command: RevokeInvitationCommand): RevokeInvitationResult {
        log.info { "Revoking invitation for user: ${command.targetUserId}" }

        val revokingUser = userRepository.findById(command.revokedByUserId)
            .orElseThrow { RecordNotFoundException(messageService.getMessage("invitation.error.user_not_found")) }

        val targetUser = userRepository.findById(command.targetUserId)
            .orElseThrow { RecordNotFoundException(messageService.getMessage("invitation.error.user_not_found")) }

        when {
            command.targetUserId == command.revokedByUserId ->
                throw InvalidRequestException(messageService.getMessage("invitation.error.cannot_revoke_self"))
            targetUser.invitationStatus ->
                throw InvalidRequestException(messageService.getMessage("invitation.error.user_already_active"))
            targetUser.merchantId != revokingUser.merchantId ->
                throw RecordNotFoundException(messageService.getMessage("invitation.error.user_not_found"))
        }

        val targetEmail = targetUser.email?.value ?: targetUser.username
        val revokingEmail = revokingUser.email?.value ?: revokingUser.username
        val revokingName = "${revokingUser.firstName ?: ""} ${revokingUser.lastName ?: ""}".trim()
            .ifEmpty { revokingEmail }
        val revokingMerchantId = revokingUser.merchantId?.toString() ?: ""

        val process = processGateway.createProcess(
            CreateNewProcessPayload(
                userId = command.revokedByUserId,
                publicId = UUID.randomUUID(),
                type = ProcessType.INVITATION_REVOCATION,
                description = "Invitation revocation for $targetEmail by $revokingEmail",
                initialState = ProcessState.PENDING,
                requestState = ProcessState.COMPLETE,
                channel = ProcessChannel.BUSINESS_WEB,
                data = mapOf(
                    ProcessRequestDataName.USER_IDENTIFIER to command.targetUserId.toString(),
                    ProcessRequestDataName.MERCHANT_ID to revokingMerchantId
                ),
                stakeholders = mapOf(
                    ProcessStakeholderType.ACTOR_USER to command.revokedByUserId.toString(),
                    ProcessStakeholderType.FOR_USER to command.targetUserId.toString()
                )
            )
        )

        log.info { "Created invitation revocation process: ${process.publicId}" }

        processGateway.findLatestPendingProcessesByTypeAndForUserId(
            ProcessType.MERCHANT_USER_INVITATION,
            command.targetUserId
        )?.let { pendingInvitation ->
            processGateway.failProcess(pendingInvitation.publicId)
            log.info { "Failed pending invitation process: ${pendingInvitation.publicId}" }
        }

        userDeletionService.deleteUser(command.targetUserId)
        log.info { "Deleted user: ${command.targetUserId}" }

        processGateway.completeProcess(process.publicId, process.requests.first().id)

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = command.revokedByUserId.toString(),
                actorName = revokingName,
                merchantId = revokingMerchantId,
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "Invitation revoked for $targetEmail",
                eventTime = Instant.now(),
                timeRecorded = Instant.now()
            )
        )

        val message = messageService.getMessage("invitation.success.revoked")
        log.info { "Successfully revoked invitation for user: ${command.targetUserId}" }

        return RevokeInvitationResult(
            success = true,
            message = message
        )
    }
}
