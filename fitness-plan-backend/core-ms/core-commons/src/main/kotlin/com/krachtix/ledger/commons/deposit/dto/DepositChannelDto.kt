package com.krachtix.deposit.dto

import javax.money.MonetaryAmount

data class DepositChannelDto(
    val id: String,
    val name: String,
    val type: String,
    val country: String?,
    val currency: String,
    val minAmount: MonetaryAmount?,
    val maxAmount: MonetaryAmount?,
    val isActive: Boolean,
    val requiresRedirect: Boolean = false
)
