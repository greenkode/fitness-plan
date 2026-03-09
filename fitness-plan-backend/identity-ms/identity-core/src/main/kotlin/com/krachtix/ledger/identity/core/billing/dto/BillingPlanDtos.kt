package com.krachtix.identity.core.billing.dto

import com.krachtix.identity.core.billing.entity.BillingCycle
import com.krachtix.identity.core.billing.entity.BillingPlanStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CreateBillingPlanRequest(
    val organizationId: UUID,
    val name: String,
    val platformFeeAmount: BigDecimal,
    val perAccountFeeAmount: BigDecimal,
    val perTransactionFeeAmount: BigDecimal,
    val currency: String = "USD",
    val billingCycle: BillingCycle,
    val effectiveFrom: Instant,
    val effectiveUntil: Instant? = null,
    val maxChargeAmount: BigDecimal? = null
)

data class UpdateBillingPlanRequest(
    val name: String? = null,
    val platformFeeAmount: BigDecimal? = null,
    val perAccountFeeAmount: BigDecimal? = null,
    val perTransactionFeeAmount: BigDecimal? = null,
    val currency: String? = null,
    val billingCycle: BillingCycle? = null,
    val effectiveUntil: Instant? = null,
    val maxChargeAmount: BigDecimal? = null
)

data class BillingPlanResponse(
    val publicId: UUID,
    val organizationId: UUID?,
    val name: String,
    val platformFeeAmount: BigDecimal,
    val perAccountFeeAmount: BigDecimal,
    val perTransactionFeeAmount: BigDecimal,
    val maxChargeAmount: BigDecimal?,
    val currency: String,
    val billingCycle: BillingCycle,
    val status: BillingPlanStatus,
    val effectiveFrom: Instant,
    val effectiveUntil: Instant?,
    val createdAt: Instant?
)

data class BillingPlanListResponse(
    val plans: List<BillingPlanResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class BillingPlanInternalResponse(
    val publicId: UUID,
    val organizationId: UUID?,
    val name: String,
    val platformFeeAmount: BigDecimal,
    val perAccountFeeAmount: BigDecimal,
    val perTransactionFeeAmount: BigDecimal,
    val maxChargeAmount: BigDecimal?,
    val currency: String,
    val billingCycle: BillingCycle,
    val status: BillingPlanStatus,
    val effectiveFrom: Instant,
    val effectiveUntil: Instant?,
    val subscriptionTier: String? = null,
    val stripePriceId: String? = null
)

data class ActivePlanWithOrgResponse(
    val organizationId: UUID?,
    val merchantId: UUID?,
    val plan: BillingPlanInternalResponse
)
