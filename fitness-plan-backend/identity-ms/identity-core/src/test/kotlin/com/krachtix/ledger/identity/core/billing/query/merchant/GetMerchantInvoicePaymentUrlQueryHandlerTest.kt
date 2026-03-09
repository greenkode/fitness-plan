package com.krachtix.identity.core.billing.query.merchant

import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.commons.billing.InvoiceResponse
import com.krachtix.identity.commons.payment.PaymentGateway
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetMerchantInvoicePaymentUrlQueryHandlerTest {

    @Mock
    private lateinit var billingDataGateway: BillingDataGateway

    @Mock
    private lateinit var paymentGateway: PaymentGateway

    @Mock
    private lateinit var userService: UserService

    private lateinit var handler: GetMerchantInvoicePaymentUrlQueryHandler

    private val merchantId = UUID.randomUUID()
    private val invoicePublicId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = GetMerchantInvoicePaymentUrlQueryHandler(billingDataGateway, paymentGateway, userService)
    }

    private fun mockCurrentUser(organizationId: UUID?) {
        val user = Mockito.mock(OAuthUser::class.java)
        whenever(user.organizationId).thenReturn(organizationId)
        whenever(userService.getCurrentUser()).thenReturn(user)
    }

    private fun buildInvoiceResponse(status: String = "ISSUED") = InvoiceResponse(
        publicId = invoicePublicId,
        periodStart = LocalDate.of(2026, 1, 1),
        periodEnd = LocalDate.of(2026, 1, 31),
        platformFeeCents = 1000L,
        accountFeeCents = 500L,
        transactionFeeCents = 200L,
        totalCents = 1700L,
        accountCount = 10,
        transactionCount = 200,
        currency = "USD",
        status = status,
        issuedAt = Instant.parse("2026-02-01T00:00:00Z"),
        dueAt = Instant.parse("2026-02-15T00:00:00Z"),
        paidAt = null,
        createdAt = Instant.parse("2026-02-01T00:00:00Z")
    )

    @Nested
    @DisplayName("Successful Payment URL Retrieval")
    inner class SuccessfulRetrieval {

        @Test
        fun `should return payment URL for ISSUED invoice`() {
            mockCurrentUser(merchantId)
            val invoice = buildInvoiceResponse(status = "ISSUED")
            whenever(billingDataGateway.getInvoice(merchantId, invoicePublicId)).thenReturn(invoice)
            whenever(paymentGateway.getInvoicePaymentUrl(invoicePublicId.toString()))
                .thenReturn("https://pay.stripe.com/inv_123")

            val result = handler.handle(GetMerchantInvoicePaymentUrlQuery(invoicePublicId))

            assertThat(result.response.url).isEqualTo("https://pay.stripe.com/inv_123")
        }

        @Test
        fun `should return payment URL for OVERDUE invoice`() {
            mockCurrentUser(merchantId)
            val invoice = buildInvoiceResponse(status = "OVERDUE")
            whenever(billingDataGateway.getInvoice(merchantId, invoicePublicId)).thenReturn(invoice)
            whenever(paymentGateway.getInvoicePaymentUrl(invoicePublicId.toString()))
                .thenReturn("https://pay.stripe.com/inv_456")

            val result = handler.handle(GetMerchantInvoicePaymentUrlQuery(invoicePublicId))

            assertThat(result.response.url).isEqualTo("https://pay.stripe.com/inv_456")
        }
    }

    @Nested
    @DisplayName("Invoice Not Found")
    inner class InvoiceNotFound {

        @Test
        fun `should throw RecordNotFoundException when invoice not found`() {
            mockCurrentUser(merchantId)
            whenever(billingDataGateway.getInvoice(merchantId, invoicePublicId)).thenReturn(null)

            assertThatThrownBy { handler.handle(GetMerchantInvoicePaymentUrlQuery(invoicePublicId)) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Invoice not found: $invoicePublicId")
        }
    }

    @Nested
    @DisplayName("Invalid Invoice Status")
    inner class InvalidInvoiceStatus {

        @Test
        fun `should throw IllegalArgumentException when invoice is PAID`() {
            mockCurrentUser(merchantId)
            val invoice = buildInvoiceResponse(status = "PAID")
            whenever(billingDataGateway.getInvoice(merchantId, invoicePublicId)).thenReturn(invoice)

            assertThatThrownBy { handler.handle(GetMerchantInvoicePaymentUrlQuery(invoicePublicId)) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PAID")
                .hasMessageContaining("payment URL only available for ISSUED or OVERDUE invoices")
        }
    }

    @Nested
    @DisplayName("No Organization")
    inner class NoOrganization {

        @Test
        fun `should throw IllegalStateException when user has no organization`() {
            mockCurrentUser(null)

            assertThatThrownBy { handler.handle(GetMerchantInvoicePaymentUrlQuery(invoicePublicId)) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User has no organization")
        }
    }
}
