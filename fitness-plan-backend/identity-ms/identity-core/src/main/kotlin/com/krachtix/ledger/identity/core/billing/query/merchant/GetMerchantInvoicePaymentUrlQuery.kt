package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import com.krachtix.identity.core.billing.dto.MerchantPaymentUrlResponse
import java.util.UUID

data class GetMerchantInvoicePaymentUrlQuery(
    val publicId: UUID
) : Command<GetMerchantInvoicePaymentUrlResult>

data class GetMerchantInvoicePaymentUrlResult(
    val response: MerchantPaymentUrlResponse
)
