package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.core.billing.dto.MerchantInvoiceListResponse
import com.krachtix.identity.core.billing.dto.MerchantInvoiceResponse
import com.krachtix.identity.core.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetMerchantInvoicesQueryHandler(
    private val billingDataGateway: BillingDataGateway,
    private val userService: UserService
) : Command.Handler<GetMerchantInvoicesQuery, GetMerchantInvoicesResult> {

    override fun handle(query: GetMerchantInvoicesQuery): GetMerchantInvoicesResult {
        val merchantId = userService.getCurrentUser().organizationId
            ?: throw IllegalStateException("User has no organization")
        log.info { "Fetching invoices for merchant=$merchantId, status=${query.status}" }

        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.DESC, "periodStart"))
        val invoicePage = billingDataGateway.getInvoices(merchantId, pageable, query.status)

        return GetMerchantInvoicesResult(
            response = MerchantInvoiceListResponse(
                invoices = invoicePage.content.map { invoice ->
                    MerchantInvoiceResponse(
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
                },
                page = invoicePage.number,
                size = invoicePage.size,
                totalElements = invoicePage.totalElements,
                totalPages = invoicePage.totalPages
            )
        )
    }
}
