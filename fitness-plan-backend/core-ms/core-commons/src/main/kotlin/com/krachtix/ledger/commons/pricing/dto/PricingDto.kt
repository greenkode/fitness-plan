package com.krachtix.pricing.dto

import javax.money.MonetaryAmount

data class PricingDto(
    val fee: MonetaryAmount, val commission: MonetaryAmount,
    val vat: MonetaryAmount, val rebate: MonetaryAmount
)