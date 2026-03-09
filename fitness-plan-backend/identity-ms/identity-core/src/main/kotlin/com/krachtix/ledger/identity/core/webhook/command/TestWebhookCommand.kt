package com.krachtix.identity.core.webhook.command

import an.awesome.pipelinr.Command
import java.util.UUID

class TestWebhookCommand(
    val publicId: UUID
) : Command<TestWebhookResult>

data class TestWebhookResult(
    val success: Boolean,
    val statusCode: Int?,
    val errorMessage: String?
)
