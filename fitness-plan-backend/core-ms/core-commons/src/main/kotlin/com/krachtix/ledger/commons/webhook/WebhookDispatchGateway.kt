package com.krachtix.commons.webhook

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.UUID

interface WebhookDeliveryGateway {
    fun getDeliveryHistory(pageable: Pageable): Page<WebhookDeliveryDto>
}

data class WebhookDeliveryDto(
    val webhookPublicId: UUID,
    val eventType: WebhookEventType,
    val eventId: UUID,
    val status: WebhookDeliveryStatus,
    val attemptCount: Int,
    val lastAttemptAt: Instant?,
    val lastStatusCode: Int?,
    val lastError: String?,
    val createdAt: Instant
)

enum class WebhookDeliveryStatus {
    PENDING,
    SUCCESS,
    FAILED,
    RETRYING
}
