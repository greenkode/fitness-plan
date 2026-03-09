package com.krachtix.commons.billing

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface BillingGateway {
    fun getActivePlan(): BillingPlanDto?
    fun getUsageSnapshots(startDate: LocalDate, endDate: LocalDate): List<UsageSnapshotDto>
    fun createUsageSnapshot(merchantId: UUID, snapshotDate: LocalDate, input: UsageSnapshotInput)
    fun getInvoices(pageable: Pageable): Page<InvoiceDto>
    fun getInvoicesByStatus(status: InvoiceStatus, pageable: Pageable): Page<InvoiceDto>
    fun getInvoice(publicId: UUID): InvoiceDto?
    fun generateInvoice(merchantId: UUID, periodStart: LocalDate, periodEnd: LocalDate): InvoiceDto
    fun issueInvoice(publicId: UUID): InvoiceDto
    fun markInvoicePaid(publicId: UUID, paidAt: Instant): InvoiceDto
    fun markInvoiceOverdue(publicId: UUID): InvoiceDto
    fun getOverdueInvoices(): List<InvoiceDto>
    fun recordPaymentEvent(input: PaymentEventInput): UUID
    fun getCurrentMonthUsage(): CurrentUsageDto
    fun getEndOfMonthProjection(): BillingProjectionDto
    fun getAllActivePlansWithMerchant(): List<BillingPlanWithMerchantDto>
    fun getMonthlyCostHistory(months: Int = 6): List<MonthlyCostDto>
    fun getCostComparison(): CostComparisonDto
}

data class BillingPlanDto(
    val publicId: UUID,
    val name: String,
    val platformFeeAmount: BigDecimal,
    val perAccountFeeAmount: BigDecimal,
    val perTransactionFeeAmount: BigDecimal,
    val maxChargeAmount: BigDecimal?,
    val currency: String,
    val billingCycle: BillingCycle,
    val status: BillingPlanStatus,
    val effectiveFrom: Instant,
    val effectiveUntil: Instant?,
    val subscriptionTier: String? = null,
    val stripePriceId: String? = null
)

data class UsageSnapshotDto(
    val snapshotDate: LocalDate,
    val activeAccountCount: Int,
    val transactionCount: Int,
    val apiCallCount: Int,
    val exportCount: Int,
    val webhookDeliveryCount: Int,
    val storageBytes: Long
)

data class UsageSnapshotInput(
    val activeAccountCount: Int,
    val transactionCount: Int,
    val apiCallCount: Int,
    val exportCount: Int,
    val webhookDeliveryCount: Int,
    val storageBytes: Long
)

data class InvoiceDto(
    val publicId: UUID,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val platformFeeCents: Long,
    val accountFeeCents: Long,
    val transactionFeeCents: Long,
    val totalCents: Long,
    val accountCount: Int,
    val transactionCount: Int,
    val currency: String,
    val status: InvoiceStatus,
    val issuedAt: Instant?,
    val dueAt: Instant?,
    val paidAt: Instant?,
    val createdAt: Instant
)

data class CurrentUsageDto(
    val activeAccounts: Int,
    val transactionCount: Int,
    val apiCallCount: Int,
    val estimatedCostCents: Long,
    val daysElapsed: Int,
    val daysInPeriod: Int
)

data class BillingProjectionDto(
    val projectedAccountFeeCents: Long,
    val projectedTransactionFeeCents: Long,
    val platformFeeCents: Long,
    val projectedTotalCents: Long
)

data class BillingPlanWithMerchantDto(
    val merchantId: UUID,
    val plan: BillingPlanDto
)

data class MonthlyCostDto(
    val yearMonth: String,
    val platformFeeCents: Long,
    val accountFeeCents: Long,
    val transactionFeeCents: Long,
    val totalCents: Long
)

data class CostComparisonDto(
    val currentMonthCostCents: Long,
    val previousMonthCostCents: Long,
    val percentageChange: Double,
    val forecastedMonthEndCostCents: Long,
    val forecastedVsPreviousPercentage: Double
)
