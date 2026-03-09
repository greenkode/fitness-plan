package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.MerchantRestrictionStatusResponse

class GetMerchantRestrictionStatusQuery : Command<GetMerchantRestrictionStatusResult>

data class GetMerchantRestrictionStatusResult(
    val response: MerchantRestrictionStatusResponse
)
