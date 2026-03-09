package com.krachtix.identity.core.webhook.command

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.service.WebhookConfigService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class RotateWebhookSecretCommandHandler(
    private val userService: UserService,
    private val webhookConfigService: WebhookConfigService,
    private val messageService: MessageService
) : Command.Handler<RotateWebhookSecretCommand, RotateWebhookSecretResult> {

    override fun handle(command: RotateWebhookSecretCommand): RotateWebhookSecretResult {
        log.info { "Processing RotateWebhookSecretCommand for publicId: ${command.publicId}" }

        val user = userService.getCurrentUser()
        val merchantId = user.merchantId
            ?: throw RecordNotFoundException(messageService.getMessage("webhook.error.merchant_not_found"))

        val (entity, secret) = webhookConfigService.rotateSecret(
            publicId = command.publicId,
            clientId = merchantId
        )

        val newSecret = String(secret)
        secret.fill('\u0000')

        log.info { "Webhook secret rotated successfully for publicId: ${entity.publicId}" }

        return RotateWebhookSecretResult(
            publicId = entity.publicId,
            newSecret = newSecret
        )
    }
}
