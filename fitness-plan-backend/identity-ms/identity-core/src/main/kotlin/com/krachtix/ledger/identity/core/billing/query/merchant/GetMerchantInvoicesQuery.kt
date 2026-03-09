package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.MerchantInvoiceListResponse

data class GetMerchantInvoicesQuery(
    val page: Int = 0,
    val size: Int = 20,
    val status: String? = null
) : Command<GetMerchantInvoicesResult>

data class GetMerchantInvoicesResult(
    val response: MerchantInvoiceListResponse
)
