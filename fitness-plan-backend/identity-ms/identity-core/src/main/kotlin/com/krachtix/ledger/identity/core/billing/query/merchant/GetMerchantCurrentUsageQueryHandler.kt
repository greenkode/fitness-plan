package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.core.billing.dto.MerchantCurrentUsageResponse
import com.krachtix.identity.core.service.UserService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetMerchantCurrentUsageQueryHandler(
    private val billingDataGateway: BillingDataGateway,
    private val userService: UserService
) : Command.Handler<GetMerchantCurrentUsageQuery, GetMerchantCurrentUsageResult> {

    override fun handle(query: GetMerchantCurrentUsageQuery): GetMerchantCurrentUsageResult {
        val merchantId = userService.getCurrentUser().organizationId
            ?: throw IllegalStateException("User has no organization")
        log.info { "Fetching current usage for merchant=$merchantId" }

        val usage = billingDataGateway.getCurrentUsage(merchantId)
        return GetMerchantCurrentUsageResult(
            usage = MerchantCurrentUsageResponse(
                activeAccounts = usage.activeAccounts,
                transactionCount = usage.transactionCount,
                apiCallCount = usage.apiCallCount,
                estimatedCostCents = usage.estimatedCostCents,
                daysElapsed = usage.daysElapsed,
                daysInPeriod = usage.daysInPeriod
            )
        )
    }
}
