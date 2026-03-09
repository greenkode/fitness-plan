package com.krachtix.identity.core.billing.command

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.entity.BillingCycle
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CreateBillingPlanCommand(
    val organizationId: UUID,
    val name: String,
    val platformFeeAmount: BigDecimal,
    val perAccountFeeAmount: BigDecimal,
    val perTransactionFeeAmount: BigDecimal,
    val maxChargeAmount: BigDecimal? = null,
    val currency: String,
    val billingCycle: BillingCycle,
    val effectiveFrom: Instant,
    val effectiveUntil: Instant? = null
) : Command<CreateBillingPlanResult>

data class CreateBillingPlanResult(
    val publicId: UUID,
    val organizationId: UUID?,
    val name: String,
    val status: String
)
