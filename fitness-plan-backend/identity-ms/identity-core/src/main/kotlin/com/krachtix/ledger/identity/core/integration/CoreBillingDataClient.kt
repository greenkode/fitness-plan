package com.krachtix.identity.core.integration

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.commons.billing.BillingProjectionResponse
import com.krachtix.identity.commons.billing.CostComparisonResponse
import com.krachtix.identity.commons.billing.CurrentUsageResponse
import com.krachtix.identity.commons.billing.InvoiceResponse
import com.krachtix.identity.commons.billing.MonthlyCostResponse
import com.krachtix.identity.commons.billing.UsageSnapshotResponse
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDate
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class CoreBillingDataClient(
    private val restClient: RestClient
) : BillingDataGateway {

    override fun getCurrentUsage(merchantId: UUID): CurrentUsageResponse {
        log.debug { "Fetching current usage from core-ms for merchant=$merchantId" }
        return restClient.get()
            .uri("/api/internal/billing/usage/current?merchantId=$merchantId")
            .retrieve()
            .body(CurrentUsageResponse::class.java)
            ?: throw IllegalStateException("Failed to fetch current usage from core-ms")
    }

    override fun getProjection(merchantId: UUID): BillingProjectionResponse {
        log.debug { "Fetching projection from core-ms for merchant=$merchantId" }
        return restClient.get()
            .uri("/api/internal/billing/projection?merchantId=$merchantId")
            .retrieve()
            .body(BillingProjectionResponse::class.java)
            ?: throw IllegalStateException("Failed to fetch projection from core-ms")
    }

    override fun getInvoices(merchantId: UUID, pageable: Pageable, status: String?): Page<InvoiceResponse> {
        log.debug { "Fetching invoices from core-ms for merchant=$merchantId, status=$status" }
        val statusParam = status?.let { "&status=$it" } ?: ""
        val response = restClient.get()
            .uri("/api/internal/billing/invoices?merchantId=$merchantId&page=${pageable.pageNumber}&size=${pageable.pageSize}$statusParam")
            .retrieve()
            .body(InvoicePageResponse::class.java)
            ?: return Page.empty()

        return PageImpl(response.content, PageRequest.of(response.number, response.size), response.totalElements)
    }

    override fun getInvoice(merchantId: UUID, publicId: UUID): InvoiceResponse? {
        log.debug { "Fetching invoice $publicId from core-ms for merchant=$merchantId" }
        return restClient.get()
            .uri("/api/internal/billing/invoices/$publicId?merchantId=$merchantId")
            .retrieve()
            .body(InvoiceResponse::class.java)
    }

    override fun getCostHistory(merchantId: UUID, months: Int): List<MonthlyCostResponse> {
        log.debug { "Fetching cost history from core-ms for merchant=$merchantId, months=$months" }
        return restClient.get()
            .uri("/api/internal/billing/cost-history?merchantId=$merchantId&months=$months")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<MonthlyCostResponse>>() {})
            ?: emptyList()
    }

    override fun getCostComparison(merchantId: UUID): CostComparisonResponse {
        log.debug { "Fetching cost comparison from core-ms for merchant=$merchantId" }
        return restClient.get()
            .uri("/api/internal/billing/cost-comparison?merchantId=$merchantId")
            .retrieve()
            .body(CostComparisonResponse::class.java)
            ?: throw IllegalStateException("Failed to fetch cost comparison from core-ms")
    }

    override fun getUsageHistory(merchantId: UUID, startDate: LocalDate, endDate: LocalDate): List<UsageSnapshotResponse> {
        log.debug { "Fetching usage history from core-ms for merchant=$merchantId, $startDate to $endDate" }
        return restClient.get()
            .uri("/api/internal/billing/usage-history?merchantId=$merchantId&startDate=$startDate&endDate=$endDate")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<UsageSnapshotResponse>>() {})
            ?: emptyList()
    }
}

private data class InvoicePageResponse(
    val content: List<InvoiceResponse> = emptyList(),
    val number: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val totalPages: Int = 0
)
