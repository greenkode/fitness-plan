package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.MerchantInvoiceResponse
import java.util.UUID

data class GetMerchantInvoiceQuery(
    val publicId: UUID
) : Command<GetMerchantInvoiceResult>

data class GetMerchantInvoiceResult(
    val invoice: MerchantInvoiceResponse
)
