package com.krachtix.identity.core.invitation.command

import com.krachtix.commons.notification.dto.MessageRecipient
import com.krachtix.commons.notification.enumeration.TemplateName
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.core.integration.NotificationClient
import com.krachtix.identity.core.invitation.dto.ResendInvitationCommand
import com.krachtix.identity.core.invitation.dto.ResendInvitationResult
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

@Component
@Transactional
class ResendInvitationCommandHandler(
    private val userRepository: OAuthUserRepository,
    private val clientRepository: OAuthRegisteredClientRepository,
    private val processGateway: ProcessGateway,
    private val notificationClient: NotificationClient,
    @Value("\${app.merchant.invitation.base-url}")
    private val invitationBaseUrl: String
) : Command.Handler<ResendInvitationCommand, ResendInvitationResult> {

    private val log = KotlinLogging.logger {}

    override fun handle(command: ResendInvitationCommand): ResendInvitationResult {
        log.info { "Processing resend invitation for email: ${command.userEmail}" }

        val resendingUser = userRepository.findById(UUID.fromString(command.resendByUserId))
            .orElseThrow { RecordNotFoundException("Resending user not found") }

        val targetUser = userRepository.findByUsername(command.userEmail)
            ?: throw RecordNotFoundException("User with email ${command.userEmail} not found")

        if (targetUser.emailVerified == true) {
            throw IllegalStateException("User has already completed the invitation process")
        }

        val merchantId = targetUser.merchantId
            ?: throw RecordNotFoundException("User is not associated with a merchant")

        val merchant = clientRepository.findById(merchantId)
            .orElseThrow { RecordNotFoundException("Merchant not found") }

        val existingProcess = processGateway.findLatestPendingProcessesByTypeAndForUserId(
            userId = targetUser.id!!,
            processType = ProcessType.MERCHANT_USER_INVITATION
        ) ?: throw RecordNotFoundException("No pending invitation found for user")

        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

        notificationClient.sendNotification(
            recipients = listOf(MessageRecipient(address = targetUser.email?.value ?: "")),
            templateName = TemplateName.MERCHANT_USER_INVITATION,
            parameters = mapOf(
                "merchant_name" to merchant.clientName,
                "invited_by" to "${resendingUser.firstName ?: ""} ${resendingUser.lastName ?: ""}".trim()
                    .ifEmpty { resendingUser.email?.value ?: "" },
                "invitation_url" to "$invitationBaseUrl?token=${existingProcess.externalReference}",
                "expiration_date" to formatter.format(
                    Instant.now().plusSeconds(existingProcess.type.timeInSeconds).atZone(ZoneId.systemDefault())
                )
            ),
            locale = Locale.ENGLISH,
            clientIdentifier = UUID.randomUUID().toString()
        )

        log.info { "Resent invitation successfully to ${targetUser.email?.value}" }

        return ResendInvitationResult(
            success = true,
            message = "Invitation resent successfully"
        )
    }
}