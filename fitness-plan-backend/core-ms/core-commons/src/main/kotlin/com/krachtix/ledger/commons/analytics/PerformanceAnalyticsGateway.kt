package com.krachtix.commons.analytics

import java.time.Instant

interface PerformanceAnalyticsGateway {
    fun getExportStats(startDate: Instant, endDate: Instant): ExportStatsDto
    fun getTransactionProcessingStats(startDate: Instant, endDate: Instant): TransactionProcessingStatsDto
    fun getWebhookDeliveryStats(startDate: Instant, endDate: Instant): WebhookDeliveryStatsDto
    fun getSystemHealthSummary(): SystemHealthDto
}

data class ExportStatsDto(
    val totalExports: Long,
    val completedCount: Long,
    val failedCount: Long,
    val totalBytes: Long
)

data class TransactionProcessingStatsDto(
    val totalTransactions: Long,
    val byType: Map<String, Long>,
    val byStatus: Map<String, Long>
)

data class WebhookDeliveryStatsDto(
    val totalDeliveries: Long,
    val successCount: Long,
    val failedCount: Long,
    val successRate: Double,
    val avgRetries: Double
)

data class SystemHealthDto(
    val activeWebhooks: Long,
    val pendingExports: Long
)
