package com.krachtix.identity.core.webhook.query

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.webhook.dto.WebhookResponse
import org.springframework.data.domain.Page

class GetWebhooksQuery(
    val page: Int,
    val size: Int
) : Command<GetWebhooksResult>

data class GetWebhooksResult(
    val webhooks: Page<WebhookResponse>
)
