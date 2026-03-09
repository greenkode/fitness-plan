package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.MerchantMonthlyCostHistoryResponse

data class GetMerchantMonthlyCostHistoryQuery(
    val months: Int = 6
) : Command<GetMerchantMonthlyCostHistoryResult>

data class GetMerchantMonthlyCostHistoryResult(
    val response: MerchantMonthlyCostHistoryResponse
)
