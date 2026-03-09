package com.krachtix.notification.service

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.tenant.TenantContext
import com.krachtix.commons.webhook.WebhookConfigGateway
import com.krachtix.commons.webhook.WebhookDeliveryDto
import com.krachtix.commons.webhook.WebhookDeliveryGateway
import com.krachtix.commons.webhook.WebhookDeliveryStatus
import com.krachtix.notification.dao.WebhookDeliveryEntity
import com.krachtix.notification.dao.WebhookDeliveryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger {}

@Component
class WebhookDeliveryService(
    private val webhookDeliveryRepository: WebhookDeliveryRepository,
    private val webhookConfigGateway: WebhookConfigGateway,
    private val restClient: RestClient
) : WebhookDeliveryGateway {

    companion object {
        private val RETRY_DELAYS_SECONDS = longArrayOf(30, 120, 600, 3600, 21600)
        private const val MAX_ATTEMPTS = 5
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    @Transactional
    fun processDeliveryBatch(batchSize: Int) {
        val retryStatuses = listOf(WebhookDeliveryStatus.PENDING, WebhookDeliveryStatus.RETRYING)
        val deliveries = webhookDeliveryRepository.findPendingRetries(
            retryStatuses,
            Instant.now(),
            PageRequest.of(0, batchSize)
        )

        log.info { "Found ${deliveries.size} webhook deliveries to process" }

        deliveries.forEach { delivery ->
            val webhook = webhookConfigGateway.getWebhookByPublicId(delivery.webhookPublicId)
            webhook?.let { attemptDelivery(delivery, it.url, it.signingSecret) }
                ?: markDeliveryFailed(delivery, "Webhook configuration no longer available")
        }
    }

    @Transactional(readOnly = true)
    override fun getDeliveryHistory(pageable: Pageable): Page<WebhookDeliveryDto> {
        val merchantId = TenantContext.getMerchantId()
        return webhookDeliveryRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable)
            .map { it.toDto() }
    }

    private fun attemptDelivery(delivery: WebhookDeliveryEntity, url: String, signingSecret: String) {
        val timestamp = Instant.now().epochSecond.toString()
        val signature = signPayload(delivery.payload, signingSecret, timestamp)

        delivery.attemptCount += 1
        delivery.lastAttemptAt = Instant.now()

        runCatching {
            val response = restClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("X-Webhook-Signature", signature)
                .header("X-Webhook-Id", delivery.eventId.toString())
                .header("X-Webhook-Timestamp", timestamp)
                .body(delivery.payload)
                .retrieve()
                .toBodilessEntity()

            delivery.lastStatusCode = response.statusCode.value()
            delivery.status = WebhookDeliveryStatus.SUCCESS
            delivery.nextRetryAt = null
            log.info { "Webhook delivery ${delivery.id} succeeded with status=${response.statusCode.value()}" }
        }.onFailure { ex ->
            log.warn { "Webhook delivery ${delivery.id} attempt ${delivery.attemptCount} failed: ${ex.message}" }
            delivery.lastError = ex.message?.take(1000)
            scheduleRetryOrFail(delivery)
        }

        webhookDeliveryRepository.save(delivery)
    }

    private fun scheduleRetryOrFail(delivery: WebhookDeliveryEntity) {
        when {
            delivery.attemptCount >= MAX_ATTEMPTS -> {
                delivery.status = WebhookDeliveryStatus.FAILED
                delivery.nextRetryAt = null
                log.warn { "Webhook delivery ${delivery.id} exhausted all $MAX_ATTEMPTS retry attempts" }
            }
            else -> {
                val delayIndex = (delivery.attemptCount - 1).coerceIn(0, RETRY_DELAYS_SECONDS.size - 1)
                delivery.status = WebhookDeliveryStatus.RETRYING
                delivery.nextRetryAt = Instant.now().plusSeconds(RETRY_DELAYS_SECONDS[delayIndex])
                log.info { "Webhook delivery ${delivery.id} scheduled for retry at ${delivery.nextRetryAt}" }
            }
        }
    }

    private fun markDeliveryFailed(delivery: WebhookDeliveryEntity, error: String) {
        delivery.status = WebhookDeliveryStatus.FAILED
        delivery.lastError = error
        delivery.nextRetryAt = null
        webhookDeliveryRepository.save(delivery)
    }

    private fun signPayload(payload: String, secret: String, timestamp: String): String {
        val data = "$timestamp.$payload"
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(secret.toByteArray(), HMAC_ALGORITHM))
        return Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray()))
    }

    private fun WebhookDeliveryEntity.toDto() = WebhookDeliveryDto(
        webhookPublicId = webhookPublicId,
        eventType = eventType,
        eventId = eventId,
        status = status,
        attemptCount = attemptCount,
        lastAttemptAt = lastAttemptAt,
        lastStatusCode = lastStatusCode,
        lastError = lastError,
        createdAt = createdAt
    )
}
