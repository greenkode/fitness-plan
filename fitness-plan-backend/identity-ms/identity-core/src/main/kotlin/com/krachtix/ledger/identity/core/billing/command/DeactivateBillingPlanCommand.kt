package com.krachtix.identity.core.billing.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class DeactivateBillingPlanCommand(
    val publicId: UUID
) : Command<DeactivateBillingPlanResult>

data class DeactivateBillingPlanResult(
    val publicId: UUID,
    val status: String
)
