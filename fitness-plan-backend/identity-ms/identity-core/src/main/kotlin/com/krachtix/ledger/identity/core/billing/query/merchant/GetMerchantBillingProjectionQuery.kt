package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.MerchantBillingProjectionResponse

class GetMerchantBillingProjectionQuery : Command<GetMerchantBillingProjectionResult>

data class GetMerchantBillingProjectionResult(
    val projection: MerchantBillingProjectionResponse
)
