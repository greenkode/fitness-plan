package com.krachtix.identity.commons.billing

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.util.UUID

interface BillingDataGateway {
    fun getCurrentUsage(merchantId: UUID): CurrentUsageResponse
    fun getProjection(merchantId: UUID): BillingProjectionResponse
    fun getInvoices(merchantId: UUID, pageable: Pageable, status: String?): Page<InvoiceResponse>
    fun getInvoice(merchantId: UUID, publicId: UUID): InvoiceResponse?
    fun getCostHistory(merchantId: UUID, months: Int): List<MonthlyCostResponse>
    fun getCostComparison(merchantId: UUID): CostComparisonResponse
    fun getUsageHistory(merchantId: UUID, startDate: LocalDate, endDate: LocalDate): List<UsageSnapshotResponse>
}
