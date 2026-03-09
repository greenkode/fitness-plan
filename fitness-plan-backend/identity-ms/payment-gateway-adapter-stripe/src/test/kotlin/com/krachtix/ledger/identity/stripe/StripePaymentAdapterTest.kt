package com.krachtix.identity.stripe

import com.stripe.model.Customer
import com.stripe.model.Invoice
import com.stripe.model.Subscription
import com.stripe.param.CustomerCreateParams
import com.stripe.param.SubscriptionCancelParams
import com.stripe.param.SubscriptionCreateParams
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class StripePaymentAdapterTest {

    private lateinit var adapter: StripePaymentService

    private lateinit var customerMockedStatic: MockedStatic<Customer>
    private lateinit var subscriptionMockedStatic: MockedStatic<Subscription>
    private lateinit var invoiceMockedStatic: MockedStatic<Invoice>

    @BeforeEach
    fun setUp() {
        customerMockedStatic = mockStatic(Customer::class.java)
        subscriptionMockedStatic = mockStatic(Subscription::class.java)
        invoiceMockedStatic = mockStatic(Invoice::class.java)
        adapter = StripePaymentService()
    }

    @AfterEach
    fun tearDown() {
        customerMockedStatic.close()
        subscriptionMockedStatic.close()
        invoiceMockedStatic.close()
    }

    @Nested
    @DisplayName("Create Customer")
    inner class CreateCustomerTests {

        @Test
        fun `should create Stripe customer and return external ID`() {
            val merchantId = UUID.randomUUID()
            val email = "test@example.com"
            val name = "Test Company"

            val mockCustomer = mock<Customer>()
            whenever(mockCustomer.id).thenReturn("cus_test123")

            customerMockedStatic.`when`<Customer> {
                Customer.create(any<CustomerCreateParams>())
            }.thenReturn(mockCustomer)

            val result = adapter.createCustomer(merchantId, email, name)

            assertEquals("cus_test123", result.externalCustomerId)
        }
    }

    @Nested
    @DisplayName("Create Subscription")
    inner class CreateSubscriptionTests {

        @Test
        fun `should create Stripe subscription and return DTO`() {
            val customerId = "cus_test123"
            val priceId = "price_abc"
            val periodEnd = 1700000000L

            val mockSubscription = mock<Subscription>()
            whenever(mockSubscription.id).thenReturn("sub_test456")
            whenever(mockSubscription.status).thenReturn("active")
            whenever(mockSubscription.currentPeriodEnd).thenReturn(periodEnd)

            subscriptionMockedStatic.`when`<Subscription> {
                Subscription.create(any<SubscriptionCreateParams>())
            }.thenReturn(mockSubscription)

            val result = adapter.createSubscription(customerId, priceId)

            assertEquals("sub_test456", result.externalSubscriptionId)
            assertEquals("active", result.status)
            assertEquals(Instant.ofEpochSecond(periodEnd), result.currentPeriodEnd)
        }
    }

    @Nested
    @DisplayName("Cancel Subscription")
    inner class CancelSubscriptionTests {

        @Test
        fun `should cancel Stripe subscription`() {
            val subscriptionId = "sub_test456"
            val mockSubscription = mock<Subscription>()

            subscriptionMockedStatic.`when`<Subscription> {
                Subscription.retrieve(subscriptionId)
            }.thenReturn(mockSubscription)

            adapter.cancelSubscription(subscriptionId)

            verify(mockSubscription).cancel(any<SubscriptionCancelParams>())
        }
    }

    @Nested
    @DisplayName("Get Invoice Payment URL")
    inner class GetInvoicePaymentUrlTests {

        @Test
        fun `should return hosted invoice URL`() {
            val invoiceId = "inv_test789"
            val expectedUrl = "https://pay.stripe.com/invoice/inv_test789"
            val mockInvoice = mock<Invoice>()
            whenever(mockInvoice.hostedInvoiceUrl).thenReturn(expectedUrl)

            invoiceMockedStatic.`when`<Invoice> {
                Invoice.retrieve(invoiceId)
            }.thenReturn(mockInvoice)

            val result = adapter.getInvoicePaymentUrl(invoiceId)

            assertEquals(expectedUrl, result)
        }

        @Test
        fun `should throw when hosted URL is null`() {
            val invoiceId = "inv_test789"
            val mockInvoice = mock<Invoice>()
            whenever(mockInvoice.hostedInvoiceUrl).thenReturn(null)

            invoiceMockedStatic.`when`<Invoice> {
                Invoice.retrieve(invoiceId)
            }.thenReturn(mockInvoice)

            val exception = assertThrows<IllegalStateException> {
                adapter.getInvoicePaymentUrl(invoiceId)
            }

            assertNotNull(exception.message)
        }
    }

    @Nested
    @DisplayName("Sync Subscription Status")
    inner class SyncSubscriptionStatusTests {

        @Test
        fun `should sync subscription status from Stripe`() {
            val subscriptionId = "sub_test456"
            val periodEnd = 1700000000L

            val mockSubscription = mock<Subscription>()
            whenever(mockSubscription.id).thenReturn("sub_test456")
            whenever(mockSubscription.status).thenReturn("past_due")
            whenever(mockSubscription.currentPeriodEnd).thenReturn(periodEnd)

            subscriptionMockedStatic.`when`<Subscription> {
                Subscription.retrieve(subscriptionId)
            }.thenReturn(mockSubscription)

            val result = adapter.syncSubscriptionStatus(subscriptionId)

            assertEquals("sub_test456", result.externalSubscriptionId)
            assertEquals("past_due", result.status)
            assertEquals(Instant.ofEpochSecond(periodEnd), result.currentPeriodEnd)
        }
    }
}
