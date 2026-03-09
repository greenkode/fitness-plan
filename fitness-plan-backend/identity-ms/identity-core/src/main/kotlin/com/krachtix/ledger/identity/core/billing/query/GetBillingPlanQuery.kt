package com.krachtix.identity.core.billing.query

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.BillingPlanResponse
import java.util.UUID

data class GetBillingPlanQuery(
    val publicId: UUID
) : Command<GetBillingPlanResult>

data class GetBillingPlanResult(
    val plan: BillingPlanResponse
)
