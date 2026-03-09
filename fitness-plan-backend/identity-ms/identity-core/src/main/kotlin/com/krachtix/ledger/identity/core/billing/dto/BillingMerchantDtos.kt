package com.krachtix.identity.core.billing.dto

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class MerchantBillingPlanResponse(
    val publicId: UUID,
    val name: String,
    val platformFeeAmount: BigDecimal,
    val perAccountFeeAmount: BigDecimal,
    val perTransactionFeeAmount: BigDecimal,
    val maxChargeAmount: BigDecimal?,
    val currency: String,
    val billingCycle: String,
    val status: String,
    val effectiveFrom: Instant,
    val effectiveUntil: Instant?
)

data class MerchantCurrentUsageResponse(
    val activeAccounts: Int,
    val transactionCount: Int,
    val apiCallCount: Int,
    val estimatedCostCents: Long,
    val daysElapsed: Int,
    val daysInPeriod: Int
)

data class MerchantBillingProjectionResponse(
    val projectedAccountFeeCents: Long,
    val projectedTransactionFeeCents: Long,
    val platformFeeCents: Long,
    val projectedTotalCents: Long
)

data class MerchantInvoiceResponse(
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
    val status: String,
    val issuedAt: Instant?,
    val dueAt: Instant?,
    val paidAt: Instant?,
    val createdAt: Instant
)

data class MerchantInvoiceListResponse(
    val invoices: List<MerchantInvoiceResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class MerchantUsageSnapshotResponse(
    val snapshotDate: LocalDate,
    val activeAccountCount: Int,
    val transactionCount: Int,
    val apiCallCount: Int,
    val exportCount: Int,
    val webhookDeliveryCount: Int,
    val storageBytes: Long
)

data class MerchantUsageHistoryResponse(
    val snapshots: List<MerchantUsageSnapshotResponse>,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class MerchantMonthlyCostResponse(
    val yearMonth: String,
    val platformFeeCents: Long,
    val accountFeeCents: Long,
    val transactionFeeCents: Long,
    val totalCents: Long
)

data class MerchantMonthlyCostHistoryResponse(
    val history: List<MerchantMonthlyCostResponse>,
    val currency: String
)

data class MerchantCostComparisonResponse(
    val currentMonthCostCents: Long,
    val previousMonthCostCents: Long,
    val percentageChange: Double,
    val forecastedMonthEndCostCents: Long,
    val forecastedVsPreviousPercentage: Double
)

data class MerchantRestrictionStatusResponse(
    val restricted: Boolean,
    val reason: String?
)

data class MerchantPaymentUrlResponse(
    val url: String
)
