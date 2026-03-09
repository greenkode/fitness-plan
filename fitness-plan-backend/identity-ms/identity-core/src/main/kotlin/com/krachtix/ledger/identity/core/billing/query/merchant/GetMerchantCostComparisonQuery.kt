package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.MerchantCostComparisonResponse

class GetMerchantCostComparisonQuery : Command<GetMerchantCostComparisonResult>

data class GetMerchantCostComparisonResult(
    val response: MerchantCostComparisonResponse
)
