package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.commons.payment.PaymentGateway
import com.krachtix.identity.core.billing.dto.MerchantPaymentUrlResponse
import com.krachtix.identity.core.service.UserService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class GetMerchantInvoicePaymentUrlQueryHandler(
    private val billingDataGateway: BillingDataGateway,
    private val paymentGateway: PaymentGateway,
    private val userService: UserService
) : Command.Handler<GetMerchantInvoicePaymentUrlQuery, GetMerchantInvoicePaymentUrlResult> {

    override fun handle(query: GetMerchantInvoicePaymentUrlQuery): GetMerchantInvoicePaymentUrlResult {
        val merchantId = userService.getCurrentUser().organizationId
            ?: throw IllegalStateException("User has no organization")
        log.info { "Fetching payment URL for invoice ${query.publicId}, merchant=$merchantId" }

        val invoice = billingDataGateway.getInvoice(merchantId, query.publicId)
            ?: throw RecordNotFoundException("Invoice not found: ${query.publicId}")

        require(invoice.status == "ISSUED" || invoice.status == "OVERDUE") {
            "Invoice ${query.publicId} is in ${invoice.status} state, payment URL only available for ISSUED or OVERDUE invoices"
        }

        val url = paymentGateway.getInvoicePaymentUrl(query.publicId.toString())

        return GetMerchantInvoicePaymentUrlResult(
            response = MerchantPaymentUrlResponse(url = url)
        )
    }
}
