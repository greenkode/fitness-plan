package com.krachtix.deposit.dto

import javax.money.MonetaryAmount

data class InitiateDepositDto(
    val channelId: String,
    val sequenceId: String,
    val amount: MonetaryAmount?,
    val localAmount: MonetaryAmount?,
    val currencyPair: String,
    val forceAccept: Boolean = false,
    val redirectUrl: String?,
    val metadata: Map<String, Any> = emptyMap()
)
