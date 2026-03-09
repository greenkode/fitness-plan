package com.krachtix.identity.core.webhook.command

import an.awesome.pipelinr.Command
import java.util.UUID

class RotateWebhookSecretCommand(
    val publicId: UUID
) : Command<RotateWebhookSecretResult>

data class RotateWebhookSecretResult(
    val publicId: UUID,
    val newSecret: String
)
