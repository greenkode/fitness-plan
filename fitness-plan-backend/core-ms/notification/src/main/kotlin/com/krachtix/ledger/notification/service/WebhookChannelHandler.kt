package com.krachtix.notification.service

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.notification.NotificationChannelHandler
import com.krachtix.commons.notification.dto.MessagePayload
import com.krachtix.commons.notification.enumeration.MessageChannel
import com.krachtix.commons.webhook.WebhookConfigGateway
import com.krachtix.commons.webhook.WebhookEventType
import com.krachtix.notification.dao.WebhookDeliveryEntity
import com.krachtix.notification.dao.WebhookDeliveryRepository
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class WebhookChannelHandler(
    private val webhookDeliveryRepository: WebhookDeliveryRepository,
    private val webhookConfigGateway: WebhookConfigGateway,
    private val objectMapper: JsonMapper
) : NotificationChannelHandler {

    override val channel = MessageChannel.WEBHOOK

    override fun handle(payload: MessagePayload) {
        val merchantId = UUID.fromString(payload.recipient.address)
        val eventType = payload.parameters["event_type"]?.toString() ?: run {
            log.warn { "Webhook payload missing event_type parameter" }
            return
        }
        val eventId = UUID.fromString(payload.clientIdentifier)

        val webhookPayload = buildEnvelope(eventType, eventId, payload.parameters)
        val payloadJson = objectMapper.writeValueAsString(webhookPayload)
        val activeWebhooks = webhookConfigGateway.getActiveWebhooks(merchantId, eventType)

        log.info { "Dispatching webhook event=$eventType, eventId=$eventId for merchant=$merchantId to ${activeWebhooks.size} webhook(s)" }

        activeWebhooks.forEach { webhook ->
            webhookDeliveryRepository.save(
                WebhookDeliveryEntity(
                    webhookPublicId = webhook.publicId,
                    eventType = WebhookEventType.valueOf(eventType),
                    eventId = eventId,
                    payload = payloadJson,
                    nextRetryAt = Instant.now()
                ).apply { this.merchantId = merchantId }
            )
        }
    }

    private fun buildEnvelope(eventType: String, eventId: UUID, data: Map<String, Any>): Map<String, Any> = mapOf(
        "id" to eventId.toString(),
        "type" to eventType,
        "timestamp" to Instant.now().toString(),
        "version" to "1.0",
        "data" to data.filterKeys { it != "event_type" }
    )
}
