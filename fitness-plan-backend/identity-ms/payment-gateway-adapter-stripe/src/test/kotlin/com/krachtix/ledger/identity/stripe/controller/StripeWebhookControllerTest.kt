package com.krachtix.identity.stripe.controller

import com.stripe.model.Customer
import com.stripe.model.Event
import com.stripe.model.EventDataObjectDeserializer
import com.stripe.model.Invoice
import com.stripe.model.StripeObject
import com.stripe.model.Subscription
import com.stripe.net.Webhook
import com.krachtix.identity.commons.payment.WebhookEventGateway
import com.krachtix.identity.commons.payment.WebhookPaymentEventRequest
import com.krachtix.identity.stripe.config.StripeConfigProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class StripeWebhookControllerTest {

    @Mock
    private lateinit var webhookEventGateway: WebhookEventGateway

    private val stripeConfigProperties = StripeConfigProperties(
        apiKey = "sk_test_dummy",
        webhookSecret = "whsec_test_dummy",
        enabled = true
    )

    private lateinit var controller: StripeWebhookController

    private val merchantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        controller = StripeWebhookController(
            webhookEventGateway,
            stripeConfigProperties
        )
    }

    private fun createMockEvent(type: String, dataObject: Any?): Event {
        val event = mock<Event>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
        Mockito.lenient().`when`(event.type).thenReturn(type)
        Mockito.lenient().`when`(event.id).thenReturn("evt_test_${UUID.randomUUID()}")
        Mockito.lenient().`when`(event.toJson()).thenReturn("{}")

        val deserializer = mock<EventDataObjectDeserializer>()
        Mockito.lenient().`when`(event.dataObjectDeserializer).thenReturn(deserializer)

        dataObject?.let {
            Mockito.lenient().`when`(deserializer.`object`).thenReturn(Optional.of(it as StripeObject))
        }

        return event
    }

    private fun createMockInvoiceWithMetadata(): Invoice {
        val invoice = mock<Invoice>()
        val customer = mock<Customer>()
        whenever(customer.metadata).thenReturn(mapOf("merchant_id" to merchantId.toString()))
        whenever(invoice.customerObject).thenReturn(customer)
        whenever(invoice.amountPaid).thenReturn(10000L)
        whenever(invoice.currency).thenReturn("usd")
        return invoice
    }

    private fun createMockSubscriptionWithMetadata(): Subscription {
        val subscription = mock<Subscription>()
        Mockito.lenient().`when`(subscription.metadata).thenReturn(mapOf("merchant_id" to merchantId.toString()))
        Mockito.lenient().`when`(subscription.id).thenReturn("sub_test_123")
        return subscription
    }

    @Nested
    @DisplayName("Invoice Paid")
    inner class InvoicePaidTests {

        @Test
        fun `should lift restriction and record payment event on invoice paid`() {
            val invoice = createMockInvoiceWithMetadata()
            val event = createMockEvent("invoice.paid", invoice)

            val webhookMock: MockedStatic<Webhook> = mockStatic(Webhook::class.java)
            webhookMock.`when`<Event> {
                Webhook.constructEvent(any(), any(), any())
            }.thenReturn(event)

            try {
                controller.handleStripeWebhook("{}", "sig_test")

                verify(webhookEventGateway).liftRestriction(merchantId)
                verify(webhookEventGateway).recordPaymentEvent(argThat<WebhookPaymentEventRequest> {
                    this.eventType == "INVOICE_PAID" && this.merchantId == merchantId
                })
            } finally {
                webhookMock.close()
            }
        }
    }

    @Nested
    @DisplayName("Invoice Payment Failed")
    inner class InvoicePaymentFailedTests {

        @Test
        fun `should record payment event on invoice payment failed`() {
            val invoice = createMockInvoiceWithMetadata()
            val event = createMockEvent("invoice.payment_failed", invoice)

            val webhookMock: MockedStatic<Webhook> = mockStatic(Webhook::class.java)
            webhookMock.`when`<Event> {
                Webhook.constructEvent(any(), any(), any())
            }.thenReturn(event)

            try {
                controller.handleStripeWebhook("{}", "sig_test")

                verify(webhookEventGateway).recordPaymentEvent(argThat<WebhookPaymentEventRequest> {
                    this.eventType == "INVOICE_PAYMENT_FAILED" && this.merchantId == merchantId
                })
            } finally {
                webhookMock.close()
            }
        }
    }

    @Nested
    @DisplayName("Subscription Updated")
    inner class SubscriptionUpdatedTests {

        @Test
        fun `should update subscription and record event`() {
            val subscription = createMockSubscriptionWithMetadata()
            val event = createMockEvent("customer.subscription.updated", subscription)

            val webhookMock: MockedStatic<Webhook> = mockStatic(Webhook::class.java)
            webhookMock.`when`<Event> {
                Webhook.constructEvent(any(), any(), any())
            }.thenReturn(event)

            try {
                controller.handleStripeWebhook("{}", "sig_test")

                verify(webhookEventGateway).updateSubscription(merchantId, "sub_test_123")
                verify(webhookEventGateway).recordPaymentEvent(argThat<WebhookPaymentEventRequest> {
                    this.eventType == "SUBSCRIPTION_UPDATED" && this.merchantId == merchantId
                })
            } finally {
                webhookMock.close()
            }
        }
    }

    @Nested
    @DisplayName("Subscription Deleted")
    inner class SubscriptionDeletedTests {

        @Test
        fun `should record payment event on subscription deleted`() {
            val subscription = createMockSubscriptionWithMetadata()
            val event = createMockEvent("customer.subscription.deleted", subscription)

            val webhookMock: MockedStatic<Webhook> = mockStatic(Webhook::class.java)
            webhookMock.`when`<Event> {
                Webhook.constructEvent(any(), any(), any())
            }.thenReturn(event)

            try {
                controller.handleStripeWebhook("{}", "sig_test")

                verify(webhookEventGateway).recordPaymentEvent(argThat<WebhookPaymentEventRequest> {
                    this.eventType == "SUBSCRIPTION_CANCELLED" && this.merchantId == merchantId
                })
            } finally {
                webhookMock.close()
            }
        }
    }

    @Nested
    @DisplayName("Unhandled Events")
    inner class UnhandledEventTests {

        @Test
        fun `should ignore unhandled event types`() {
            val event = createMockEvent("charge.succeeded", null)

            val webhookMock: MockedStatic<Webhook> = mockStatic(Webhook::class.java)
            webhookMock.`when`<Event> {
                Webhook.constructEvent(any(), any(), any())
            }.thenReturn(event)

            try {
                controller.handleStripeWebhook("{}", "sig_test")

                verify(webhookEventGateway, never()).recordPaymentEvent(any())
            } finally {
                webhookMock.close()
            }
        }
    }
}
