package com.krachtix.identity.core.webhook.command

import an.awesome.pipelinr.Command
import java.util.UUID

class DeleteWebhookCommand(
    val publicId: UUID
) : Command<DeleteWebhookResult>

data class DeleteWebhookResult(
    val success: Boolean
)
