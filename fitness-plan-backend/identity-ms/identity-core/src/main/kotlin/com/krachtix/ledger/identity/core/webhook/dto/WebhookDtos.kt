package com.krachtix.identity.core.webhook.dto

import com.krachtix.identity.core.entity.WebhookConfigStatus
import java.time.Instant
import java.util.UUID

data class CreateWebhookRequest(
    val name: String,
    val url: String,
    val eventTypes: List<String>,
    val description: String? = null
)

data class CreateWebhookResponse(
    val publicId: UUID,
    val name: String,
    val url: String,
    val signingSecret: String,
    val eventTypes: List<String>,
    val description: String?,
    val status: WebhookConfigStatus
)

data class UpdateWebhookRequest(
    val name: String? = null,
    val url: String? = null,
    val eventTypes: List<String>? = null,
    val description: String? = null
)

data class WebhookResponse(
    val publicId: UUID,
    val name: String,
    val url: String,
    val eventTypes: List<String>,
    val description: String?,
    val status: WebhookConfigStatus,
    val createdAt: Instant?
)

data class WebhookListResponse(
    val webhooks: List<WebhookResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class RotateWebhookSecretResponse(
    val publicId: UUID,
    val newSecret: String
)

data class TestWebhookResponse(
    val success: Boolean,
    val statusCode: Int?,
    val errorMessage: String?
)

data class ActiveWebhookDto(
    val publicId: UUID,
    val url: String,
    val signingSecret: String,
    val eventTypes: List<String>
)
