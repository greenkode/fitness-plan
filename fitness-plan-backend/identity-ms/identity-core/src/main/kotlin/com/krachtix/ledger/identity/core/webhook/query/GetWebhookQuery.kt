package com.krachtix.identity.core.webhook.query

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.webhook.dto.WebhookResponse
import java.util.UUID

class GetWebhookQuery(
    val publicId: UUID
) : Command<GetWebhookResult>

data class GetWebhookResult(
    val webhook: WebhookResponse
)
