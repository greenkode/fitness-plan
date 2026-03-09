package com.krachtix.identity.stripe.controller

import com.stripe.model.Event
import com.stripe.model.Invoice
import com.stripe.model.Subscription
import com.stripe.net.Webhook
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.commons.payment.WebhookEventGateway
import com.krachtix.identity.commons.payment.WebhookPaymentEventRequest
import com.krachtix.identity.stripe.config.StripeConfigProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/webhooks")
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class StripeWebhookController(
    private val webhookEventGateway: WebhookEventGateway,
    private val stripeConfigProperties: StripeConfigProperties
) {

    @PostMapping("/stripe")
    @ResponseStatus(HttpStatus.OK)
    fun handleStripeWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signature: String
    ) {
        val event = verifyAndParseEvent(payload, signature)
        log.info { "Received Stripe webhook event: ${event.type} id=${event.id}" }

        when (event.type) {
            "invoice.paid" -> handleInvoicePaid(event)
            "invoice.payment_failed" -> handleInvoicePaymentFailed(event)
            "customer.subscription.updated" -> handleSubscriptionUpdated(event)
            "customer.subscription.deleted" -> handleSubscriptionDeleted(event)
            else -> log.debug { "Ignoring unhandled Stripe event type: ${event.type}" }
        }
    }

    private fun verifyAndParseEvent(payload: String, signature: String): Event =
        Webhook.constructEvent(payload, signature, stripeConfigProperties.webhookSecret)

    private fun handleInvoicePaid(event: Event) {
        val merchantId = extractMerchantId(event) ?: return

        webhookEventGateway.liftRestriction(merchantId)

        webhookEventGateway.recordPaymentEvent(
            WebhookPaymentEventRequest(
                externalEventId = event.id,
                eventType = "INVOICE_PAID",
                merchantId = merchantId,
                invoicePublicId = null,
                amountCents = extractAmountCents(event),
                currency = extractCurrency(event),
                rawPayload = event.toJson()
            )
        )

        log.info { "Processed invoice.paid for merchant $merchantId" }
    }

    private fun handleInvoicePaymentFailed(event: Event) {
        val merchantId = extractMerchantId(event) ?: return

        webhookEventGateway.recordPaymentEvent(
            WebhookPaymentEventRequest(
                externalEventId = event.id,
                eventType = "INVOICE_PAYMENT_FAILED",
                merchantId = merchantId,
                invoicePublicId = null,
                amountCents = extractAmountCents(event),
                currency = extractCurrency(event),
                rawPayload = event.toJson()
            )
        )

        log.warn { "Invoice payment failed for merchant $merchantId, event=${event.id}" }
    }

    private fun handleSubscriptionUpdated(event: Event) {
        val merchantId = extractMerchantId(event) ?: return
        val subscription = extractSubscription(event)

        subscription?.id?.let { webhookEventGateway.updateSubscription(merchantId, it) }

        webhookEventGateway.recordPaymentEvent(
            WebhookPaymentEventRequest(
                externalEventId = event.id,
                eventType = "SUBSCRIPTION_UPDATED",
                merchantId = merchantId,
                invoicePublicId = null,
                amountCents = null,
                currency = null,
                rawPayload = event.toJson()
            )
        )

        log.info { "Processed subscription.updated for merchant $merchantId" }
    }

    private fun handleSubscriptionDeleted(event: Event) {
        val merchantId = extractMerchantId(event) ?: return

        webhookEventGateway.recordPaymentEvent(
            WebhookPaymentEventRequest(
                externalEventId = event.id,
                eventType = "SUBSCRIPTION_CANCELLED",
                merchantId = merchantId,
                invoicePublicId = null,
                amountCents = null,
                currency = null,
                rawPayload = event.toJson()
            )
        )

        log.info { "Processed subscription.deleted for merchant $merchantId" }
    }

    private fun extractMerchantId(event: Event): UUID? {
        val dataObject = event.dataObjectDeserializer?.`object`?.orElse(null)
        val metadata = when (dataObject) {
            is Invoice -> dataObject.customerObject?.metadata
            is Subscription -> dataObject.metadata
            else -> null
        }
        val merchantIdStr = metadata?.get("merchant_id")
        return merchantIdStr?.let {
            try {
                UUID.fromString(it)
            } catch (_: IllegalArgumentException) {
                log.warn { "Invalid merchant_id in Stripe event metadata: $it" }
                null
            }
        }
    }

    private fun extractAmountCents(event: Event): Long? {
        val dataObject = event.dataObjectDeserializer?.`object`?.orElse(null)
        return when (dataObject) {
            is Invoice -> dataObject.amountPaid
            else -> null
        }
    }

    private fun extractCurrency(event: Event): String? {
        val dataObject = event.dataObjectDeserializer?.`object`?.orElse(null)
        return when (dataObject) {
            is Invoice -> dataObject.currency
            else -> null
        }
    }

    private fun extractSubscription(event: Event): Subscription? {
        val dataObject = event.dataObjectDeserializer?.`object`?.orElse(null)
        return dataObject as? Subscription
    }
}
