package com.krachtix.identity.commons.dto

import java.time.temporal.ChronoUnit

data class RateLimitConfigDto(
    val methodName: String,
    val subscriptionTier: String,
    val scope: String,
    val capacity: Int,
    val timeValue: Int,
    val timeUnit: ChronoUnit
)

data class RateLimitConfigsResponse(
    val configs: List<RateLimitConfigDto>
)
