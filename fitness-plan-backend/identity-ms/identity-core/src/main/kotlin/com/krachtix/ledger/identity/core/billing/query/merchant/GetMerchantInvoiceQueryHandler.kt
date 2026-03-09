package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.core.billing.dto.MerchantInvoiceResponse
import com.krachtix.identity.core.service.UserService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetMerchantInvoiceQueryHandler(
    private val billingDataGateway: BillingDataGateway,
    private val userService: UserService
) : Command.Handler<GetMerchantInvoiceQuery, GetMerchantInvoiceResult> {

    override fun handle(query: GetMerchantInvoiceQuery): GetMerchantInvoiceResult {
        val merchantId = userService.getCurrentUser().organizationId
            ?: throw IllegalStateException("User has no organization")
        log.info { "Fetching invoice ${query.publicId} for merchant=$merchantId" }

        val invoice = billingDataGateway.getInvoice(merchantId, query.publicId)
            ?: throw RecordNotFoundException("Invoice not found: ${query.publicId}")

        return GetMerchantInvoiceResult(
            invoice = MerchantInvoiceResponse(
                publicId = invoice.publicId,
                periodStart = invoice.periodStart,
                periodEnd = invoice.periodEnd,
                platformFeeCents = invoice.platformFeeCents,
                accountFeeCents = invoice.accountFeeCents,
                transactionFeeCents = invoice.transactionFeeCents,
                totalCents = invoice.totalCents,
                accountCount = invoice.accountCount,
                transactionCount = invoice.transactionCount,
                currency = invoice.currency,
                status = invoice.status,
                issuedAt = invoice.issuedAt,
                dueAt = invoice.dueAt,
                paidAt = invoice.paidAt,
                createdAt = invoice.createdAt
            )
        )
    }
}
