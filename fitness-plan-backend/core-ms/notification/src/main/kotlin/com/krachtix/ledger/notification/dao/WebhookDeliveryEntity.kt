package com.krachtix.notification.dao

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import com.krachtix.commons.model.TenantAwareEntity
import com.krachtix.commons.webhook.WebhookDeliveryStatus
import com.krachtix.commons.webhook.WebhookEventType
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "webhook_delivery")
class WebhookDeliveryEntity(
    val webhookPublicId: UUID,
    @Enumerated(EnumType.STRING)
    val eventType: WebhookEventType,
    val eventId: UUID,
    val payload: String,
    @Enumerated(EnumType.STRING)
    var status: WebhookDeliveryStatus = WebhookDeliveryStatus.PENDING,
    var attemptCount: Int = 0,
    var lastAttemptAt: Instant? = null,
    var nextRetryAt: Instant? = Instant.now(),
    var lastStatusCode: Int? = null,
    var lastError: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) : TenantAwareEntity(), Serializable
