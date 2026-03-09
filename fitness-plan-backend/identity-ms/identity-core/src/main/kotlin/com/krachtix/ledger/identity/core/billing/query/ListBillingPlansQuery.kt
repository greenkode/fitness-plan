package com.krachtix.identity.core.billing.query

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.BillingPlanListResponse
import com.krachtix.identity.core.billing.entity.BillingPlanStatus
import java.util.UUID

data class ListBillingPlansQuery(
    val organizationId: UUID? = null,
    val status: BillingPlanStatus? = null,
    val page: Int = 0,
    val size: Int = 20
) : Command<ListBillingPlansResult>

data class ListBillingPlansResult(
    val response: BillingPlanListResponse
)
