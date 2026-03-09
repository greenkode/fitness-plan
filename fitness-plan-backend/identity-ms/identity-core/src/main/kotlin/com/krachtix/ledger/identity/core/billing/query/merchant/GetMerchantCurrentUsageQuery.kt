package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.MerchantCurrentUsageResponse

class GetMerchantCurrentUsageQuery : Command<GetMerchantCurrentUsageResult>

data class GetMerchantCurrentUsageResult(
    val usage: MerchantCurrentUsageResponse
)
