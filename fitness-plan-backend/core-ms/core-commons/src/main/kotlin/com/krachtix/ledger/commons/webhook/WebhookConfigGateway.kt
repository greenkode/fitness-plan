package com.krachtix.commons.webhook

import java.io.Serializable
import java.util.UUID

interface WebhookConfigGateway {
    fun getActiveWebhooks(merchantId: UUID, eventType: String): List<WebhookConfigDto>
    fun getWebhookByPublicId(publicId: UUID): WebhookConfigDto?
}

data class WebhookConfigDto(
    val publicId: UUID,
    val url: String,
    val signingSecret: String,
    val eventTypes: List<String>
) : Serializable
