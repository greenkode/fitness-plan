package com.krachtix.identity.core.webhook.command

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.entity.WebhookConfigStatus
import java.util.UUID

class CreateWebhookCommand(
    val name: String,
    val url: String,
    val eventTypes: List<String>,
    val description: String?
) : Command<CreateWebhookResult>

data class CreateWebhookResult(
    val publicId: UUID,
    val name: String,
    val url: String,
    val signingSecret: String,
    val eventTypes: List<String>,
    val description: String?,
    val status: WebhookConfigStatus
)
