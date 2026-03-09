package com.krachtix.pricing.dto

import java.time.Instant
import javax.money.MonetaryAmount

data class PricingParameterDto(
    val amount: MonetaryAmount,
    val transactionType: String,
    val accountChargedId: String,
    val transactionTime: Instant,
    val integratorId: String? = null
)
