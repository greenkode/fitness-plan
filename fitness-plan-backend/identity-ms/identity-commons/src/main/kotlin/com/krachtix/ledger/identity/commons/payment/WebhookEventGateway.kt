package com.krachtix.identity.commons.payment

import java.util.UUID

interface WebhookEventGateway {
    fun recordPaymentEvent(request: WebhookPaymentEventRequest)
    fun liftRestriction(merchantId: UUID)
    fun updateSubscription(merchantId: UUID, subscriptionId: String)
}

data class WebhookPaymentEventRequest(
    val externalEventId: String,
    val eventType: String,
    val merchantId: UUID,
    val invoicePublicId: UUID?,
    val amountCents: Long?,
    val currency: String?,
    val rawPayload: String?
)
