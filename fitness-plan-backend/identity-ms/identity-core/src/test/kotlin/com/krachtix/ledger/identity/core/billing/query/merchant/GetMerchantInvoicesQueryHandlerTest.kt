package com.krachtix.identity.core.billing.query.merchant

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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetMerchantInvoicesQueryHandlerTest {

    @Mock
    private lateinit var billingDataGateway: BillingDataGateway

    @Mock
    private lateinit var userService: UserService

    private lateinit var handler: GetMerchantInvoicesQueryHandler

    private val merchantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = GetMerchantInvoicesQueryHandler(billingDataGateway, userService)
    }

    private fun mockCurrentUser(organizationId: UUID?) {
        val user = Mockito.mock(OAuthUser::class.java)
        whenever(user.organizationId).thenReturn(organizationId)
        whenever(userService.getCurrentUser()).thenReturn(user)
    }

    private fun buildInvoiceResponse(
        publicId: UUID = UUID.randomUUID(),
        status: String = "ISSUED"
    ) = InvoiceResponse(
        publicId = publicId,
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
    @DisplayName("Successful Invoice List Retrieval")
    inner class SuccessfulRetrieval {

        @Test
        fun `should return paginated invoices with mapped fields`() {
            mockCurrentUser(merchantId)
            val invoicePublicId = UUID.randomUUID()
            val invoice = buildInvoiceResponse(publicId = invoicePublicId)
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "periodStart"))
            val page = PageImpl(listOf(invoice), pageable, 1L)
            whenever(billingDataGateway.getInvoices(eq(merchantId), any(), eq(null))).thenReturn(page)

            val result = handler.handle(GetMerchantInvoicesQuery())

            assertThat(result.response.invoices).hasSize(1)
            val mapped = result.response.invoices.first()
            assertThat(mapped.publicId).isEqualTo(invoicePublicId)
            assertThat(mapped.periodStart).isEqualTo(LocalDate.of(2026, 1, 1))
            assertThat(mapped.periodEnd).isEqualTo(LocalDate.of(2026, 1, 31))
            assertThat(mapped.platformFeeCents).isEqualTo(1000L)
            assertThat(mapped.accountFeeCents).isEqualTo(500L)
            assertThat(mapped.transactionFeeCents).isEqualTo(200L)
            assertThat(mapped.totalCents).isEqualTo(1700L)
            assertThat(mapped.accountCount).isEqualTo(10)
            assertThat(mapped.transactionCount).isEqualTo(200)
            assertThat(mapped.currency).isEqualTo("USD")
            assertThat(mapped.status).isEqualTo("ISSUED")
            assertThat(mapped.issuedAt).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"))
            assertThat(mapped.dueAt).isEqualTo(Instant.parse("2026-02-15T00:00:00Z"))
            assertThat(mapped.paidAt).isNull()
            assertThat(mapped.createdAt).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"))
            assertThat(result.response.page).isEqualTo(0)
            assertThat(result.response.size).isEqualTo(20)
            assertThat(result.response.totalElements).isEqualTo(1L)
            assertThat(result.response.totalPages).isEqualTo(1)
        }

        @Test
        fun `should handle empty page`() {
            mockCurrentUser(merchantId)
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "periodStart"))
            val emptyPage = PageImpl<InvoiceResponse>(emptyList(), pageable, 0L)
            whenever(billingDataGateway.getInvoices(eq(merchantId), any(), eq(null))).thenReturn(emptyPage)

            val result = handler.handle(GetMerchantInvoicesQuery())

            assertThat(result.response.invoices).isEmpty()
            assertThat(result.response.totalElements).isEqualTo(0L)
            assertThat(result.response.totalPages).isEqualTo(0)
        }

        @Test
        fun `should pass status filter to gateway`() {
            mockCurrentUser(merchantId)
            val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "periodStart"))
            val page = PageImpl<InvoiceResponse>(emptyList(), pageable, 0L)
            whenever(billingDataGateway.getInvoices(eq(merchantId), any(), eq("PAID"))).thenReturn(page)

            handler.handle(GetMerchantInvoicesQuery(page = 0, size = 10, status = "PAID"))

            verify(billingDataGateway).getInvoices(eq(merchantId), any(), eq("PAID"))
        }
    }

    @Nested
    @DisplayName("No Organization")
    inner class NoOrganization {

        @Test
        fun `should throw IllegalStateException when user has no organization`() {
            mockCurrentUser(null)

            assertThatThrownBy { handler.handle(GetMerchantInvoicesQuery()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User has no organization")
        }
    }
}
