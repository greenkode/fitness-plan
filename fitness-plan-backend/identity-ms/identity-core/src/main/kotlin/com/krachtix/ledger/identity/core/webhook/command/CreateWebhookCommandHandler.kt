package com.krachtix.identity.core.webhook.command

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.service.WebhookConfigService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class CreateWebhookCommandHandler(
    private val userService: UserService,
    private val webhookConfigService: WebhookConfigService,
    private val processGateway: ProcessGateway,
    private val messageService: MessageService
) : Command.Handler<CreateWebhookCommand, CreateWebhookResult> {

    override fun handle(command: CreateWebhookCommand): CreateWebhookResult {
        log.info { "Processing CreateWebhookCommand" }

        val user = userService.getCurrentUser()
        val merchantId = user.merchantId
            ?: throw RecordNotFoundException(messageService.getMessage("webhook.error.merchant_not_found"))

        val processPublicId = UUID.randomUUID()
        processGateway.createProcess(
            CreateNewProcessPayload(
                userId = user.id!!,
                publicId = processPublicId,
                type = ProcessType.WEBHOOK_CREATION,
                description = "Webhook configuration creation",
                initialState = ProcessState.PENDING,
                requestState = ProcessState.PENDING,
                channel = ProcessChannel.BUSINESS_WEB,
                data = mapOf(
                    ProcessRequestDataName.WEBHOOK_URL to command.url,
                    ProcessRequestDataName.MERCHANT_ID to merchantId.toString()
                ),
                stakeholders = mapOf(
                    ProcessStakeholderType.ACTOR_USER to user.id.toString()
                )
            )
        )

        val (entity, secret) = webhookConfigService.createWebhook(
            clientId = merchantId,
            name = command.name,
            url = command.url,
            eventTypes = command.eventTypes,
            description = command.description
        )

        val signingSecret = String(secret)
        secret.fill('\u0000')

        processGateway.completeProcess(processPublicId, 1L)

        log.info { "Webhook created successfully with publicId: ${entity.publicId}" }

        return CreateWebhookResult(
            publicId = entity.publicId,
            name = entity.name,
            url = entity.url,
            signingSecret = signingSecret,
            eventTypes = entity.getEventTypeList(),
            description = entity.description,
            status = entity.status
        )
    }
}
