package com.krachtix.identity.core.integration

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class CoreBillingClient(
    private val restClient: RestClient
) {

    fun recordPaymentEvent(input: PaymentEventRequest) {
        log.info { "Recording payment event in core-ms: eventType=${input.eventType}, externalEventId=${input.externalEventId}" }

        restClient.post()
            .uri("/api/internal/billing/payment-events")
            .body(input)
            .retrieve()
            .toBodilessEntity()

        log.info { "Payment event recorded in core-ms: ${input.externalEventId}" }
    }
}

data class PaymentEventRequest(
    val externalEventId: String,
    val eventType: String,
    val merchantId: UUID,
    val invoicePublicId: UUID?,
    val amountCents: Long?,
    val currency: String?,
    val rawPayload: String?
)
