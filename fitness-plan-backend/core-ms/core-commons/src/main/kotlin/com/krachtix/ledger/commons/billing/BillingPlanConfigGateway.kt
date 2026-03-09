package com.krachtix.commons.billing

import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

interface BillingPlanConfigGateway {
    fun getActivePlanForMerchant(merchantId: UUID): BillingPlanConfigDto?
    fun getAllActivePlansWithMerchant(): List<BillingPlanConfigWithMerchantDto>
}

data class BillingPlanConfigDto(
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
    val effectiveUntil: Instant?,
    val subscriptionTier: String? = null,
    val stripePriceId: String? = null
) : Serializable

data class BillingPlanConfigWithMerchantDto(
    val merchantId: UUID,
    val plan: BillingPlanConfigDto
) : Serializable
