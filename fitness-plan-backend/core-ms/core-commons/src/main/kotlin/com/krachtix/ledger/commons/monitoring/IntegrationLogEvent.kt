package com.krachtix.monitoring

import java.time.Instant
import java.util.UUID

data class IntegrationLogEvent(
    val integrationId: String,
    val methodName: String,
    val requestBody: String?,
    val responseBody: String?,
    val errorMessage: String?,
    val errorType: String?,
    val durationMs: Long,
    val status: IntegrationStatus,
    val direction: IntegrationDirection,
    val processReference: UUID?,
    val timestamp: Instant = Instant.now()
)
