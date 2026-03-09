package com.krachtix.identity.core.billing.query.merchant

import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.commons.billing.InvoiceResponse
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
class GetMerchantInvoiceQueryHandlerTest {

    @Mock
    private lateinit var billingDataGateway: BillingDataGateway

    @Mock
    private lateinit var userService: UserService

    private lateinit var handler: GetMerchantInvoiceQueryHandler

    private val merchantId = UUID.randomUUID()
    private val invoicePublicId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = GetMerchantInvoiceQueryHandler(billingDataGateway, userService)
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
    @DisplayName("Successful Invoice Retrieval")
    inner class SuccessfulRetrieval {

        @Test
        fun `should return invoice with all fields mapped`() {
            mockCurrentUser(merchantId)
            val invoice = buildInvoiceResponse()
            whenever(billingDataGateway.getInvoice(merchantId, invoicePublicId)).thenReturn(invoice)

            val result = handler.handle(GetMerchantInvoiceQuery(invoicePublicId))

            assertThat(result.invoice.publicId).isEqualTo(invoicePublicId)
            assertThat(result.invoice.periodStart).isEqualTo(LocalDate.of(2026, 1, 1))
            assertThat(result.invoice.periodEnd).isEqualTo(LocalDate.of(2026, 1, 31))
            assertThat(result.invoice.platformFeeCents).isEqualTo(1000L)
            assertThat(result.invoice.accountFeeCents).isEqualTo(500L)
            assertThat(result.invoice.transactionFeeCents).isEqualTo(200L)
            assertThat(result.invoice.totalCents).isEqualTo(1700L)
            assertThat(result.invoice.accountCount).isEqualTo(10)
            assertThat(result.invoice.transactionCount).isEqualTo(200)
            assertThat(result.invoice.currency).isEqualTo("USD")
            assertThat(result.invoice.status).isEqualTo("ISSUED")
            assertThat(result.invoice.issuedAt).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"))
            assertThat(result.invoice.dueAt).isEqualTo(Instant.parse("2026-02-15T00:00:00Z"))
            assertThat(result.invoice.paidAt).isNull()
            assertThat(result.invoice.createdAt).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"))
        }
    }

    @Nested
    @DisplayName("Invoice Not Found")
    inner class InvoiceNotFound {

        @Test
        fun `should throw RecordNotFoundException when invoice not found`() {
            mockCurrentUser(merchantId)
            whenever(billingDataGateway.getInvoice(merchantId, invoicePublicId)).thenReturn(null)

            assertThatThrownBy { handler.handle(GetMerchantInvoiceQuery(invoicePublicId)) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Invoice not found: $invoicePublicId")
        }
    }

    @Nested
    @DisplayName("No Organization")
    inner class NoOrganization {

        @Test
        fun `should throw IllegalStateException when user has no organization`() {
            mockCurrentUser(null)

            assertThatThrownBy { handler.handle(GetMerchantInvoiceQuery(invoicePublicId)) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User has no organization")
        }
    }
}
