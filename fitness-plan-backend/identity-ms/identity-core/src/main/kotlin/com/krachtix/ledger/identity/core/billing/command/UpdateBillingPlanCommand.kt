package com.krachtix.identity.core.billing.command

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.entity.BillingCycle
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class UpdateBillingPlanCommand(
    val publicId: UUID,
    val name: String? = null,
    val platformFeeAmount: BigDecimal? = null,
    val perAccountFeeAmount: BigDecimal? = null,
    val perTransactionFeeAmount: BigDecimal? = null,
    val maxChargeAmount: BigDecimal? = null,
    val currency: String? = null,
    val billingCycle: BillingCycle? = null,
    val effectiveUntil: Instant? = null
) : Command<UpdateBillingPlanResult>

data class UpdateBillingPlanResult(
    val publicId: UUID,
    val name: String,
    val status: String
)
