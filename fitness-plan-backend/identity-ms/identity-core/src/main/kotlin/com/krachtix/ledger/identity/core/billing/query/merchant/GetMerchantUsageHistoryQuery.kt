package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.MerchantUsageHistoryResponse
import java.time.LocalDate

data class GetMerchantUsageHistoryQuery(
    val startDate: LocalDate,
    val endDate: LocalDate
) : Command<GetMerchantUsageHistoryResult>

data class GetMerchantUsageHistoryResult(
    val response: MerchantUsageHistoryResponse
)
