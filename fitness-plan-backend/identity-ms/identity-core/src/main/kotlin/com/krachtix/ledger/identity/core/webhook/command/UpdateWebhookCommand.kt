package com.krachtix.identity.core.webhook.command

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.entity.WebhookConfigStatus
import java.util.UUID

class UpdateWebhookCommand(
    val publicId: UUID,
    val name: String?,
    val url: String?,
    val eventTypes: List<String>?,
    val description: String?
) : Command<UpdateWebhookResult>

data class UpdateWebhookResult(
    val publicId: UUID,
    val name: String,
    val url: String,
    val eventTypes: List<String>,
    val description: String?,
    val status: WebhookConfigStatus
)
