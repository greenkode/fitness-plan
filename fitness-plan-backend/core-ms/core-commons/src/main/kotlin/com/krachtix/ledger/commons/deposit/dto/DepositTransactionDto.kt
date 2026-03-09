package com.krachtix.deposit.dto

import java.math.BigDecimal
import java.time.Instant
import javax.money.MonetaryAmount

data class DepositTransactionDto(
    val id: String,
    val status: DepositTransactionStatus,
    val channelId: String,
    val sequenceId: String,
    val amount: MonetaryAmount?,
    val localAmount: MonetaryAmount?,
    val exchangeRate: BigDecimal?,
    val fee: MonetaryAmount?,
    val paymentDetails: PaymentDetailsDto?,
    val reference: String?,
    val redirectUrl: String?,
    val expiresAt: Instant?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

data class PaymentDetailsDto(
    val bankName: String?,
    val accountNumber: String?,
    val accountName: String?,
    val routingNumber: String?,
    val instructions: String?
)
