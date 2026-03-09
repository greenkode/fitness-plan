package com.krachtix.identity.commons.billing

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CurrentUsageResponse(
    val activeAccounts: Int,
    val transactionCount: Int,
    val apiCallCount: Int,
    val estimatedCostCents: Long,
    val daysElapsed: Int,
    val daysInPeriod: Int
)

data class BillingProjectionResponse(
    val projectedAccountFeeCents: Long,
    val projectedTransactionFeeCents: Long,
    val platformFeeCents: Long,
    val projectedTotalCents: Long
)

data class InvoiceResponse(
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

data class MonthlyCostResponse(
    val yearMonth: String,
    val platformFeeCents: Long,
    val accountFeeCents: Long,
    val transactionFeeCents: Long,
    val totalCents: Long
)

data class CostComparisonResponse(
    val currentMonthCostCents: Long,
    val previousMonthCostCents: Long,
    val percentageChange: Double,
    val forecastedMonthEndCostCents: Long,
    val forecastedVsPreviousPercentage: Double
)

data class UsageSnapshotResponse(
    val snapshotDate: LocalDate,
    val activeAccountCount: Int,
    val transactionCount: Int,
    val apiCallCount: Int,
    val exportCount: Int,
    val webhookDeliveryCount: Int,
    val storageBytes: Long
)
