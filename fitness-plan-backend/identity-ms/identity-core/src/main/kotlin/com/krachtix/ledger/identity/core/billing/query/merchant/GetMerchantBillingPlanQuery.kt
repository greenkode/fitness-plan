package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.MerchantBillingPlanResponse

class GetMerchantBillingPlanQuery : Command<GetMerchantBillingPlanResult>

data class GetMerchantBillingPlanResult(
    val plan: MerchantBillingPlanResponse?
)
