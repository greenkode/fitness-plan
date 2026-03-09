package com.krachtix.identity.core.webhook.query

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.service.WebhookConfigService
import com.krachtix.identity.core.webhook.dto.WebhookResponse
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetWebhooksQueryHandler(
    private val userService: UserService,
    private val webhookConfigService: WebhookConfigService,
    private val messageService: MessageService
) : Command.Handler<GetWebhooksQuery, GetWebhooksResult> {

    override fun handle(query: GetWebhooksQuery): GetWebhooksResult {
        log.info { "Processing GetWebhooksQuery" }

        val user = userService.getCurrentUser()
        val merchantId = user.merchantId
            ?: throw RecordNotFoundException(messageService.getMessage("webhook.error.merchant_not_found"))

        val page = webhookConfigService.listWebhooks(merchantId, query.page, query.size)

        val mapped = page.map { entity ->
            WebhookResponse(
                publicId = entity.publicId,
                name = entity.name,
                url = entity.url,
                eventTypes = entity.getEventTypeList(),
                description = entity.description,
                status = entity.status,
                createdAt = entity.createdAt
            )
        }

        return GetWebhooksResult(webhooks = mapped)
    }
}
