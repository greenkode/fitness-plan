package com.krachtix.commons.billing

import java.util.UUID

data class PaymentEventInput(
    val externalEventId: String,
    val eventType: PaymentEventType,
    val invoicePublicId: UUID?,
    val amountCents: Long?,
    val currency: String?,
    val rawPayload: String?
)
