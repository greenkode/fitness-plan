package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.core.billing.dto.MerchantCostComparisonResponse
import com.krachtix.identity.core.service.UserService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetMerchantCostComparisonQueryHandler(
    private val billingDataGateway: BillingDataGateway,
    private val userService: UserService
) : Command.Handler<GetMerchantCostComparisonQuery, GetMerchantCostComparisonResult> {

    override fun handle(query: GetMerchantCostComparisonQuery): GetMerchantCostComparisonResult {
        val merchantId = userService.getCurrentUser().organizationId
            ?: throw IllegalStateException("User has no organization")
        log.info { "Fetching cost comparison for merchant=$merchantId" }

        val comparison = billingDataGateway.getCostComparison(merchantId)
        return GetMerchantCostComparisonResult(
            response = MerchantCostComparisonResponse(
                currentMonthCostCents = comparison.currentMonthCostCents,
                previousMonthCostCents = comparison.previousMonthCostCents,
                percentageChange = comparison.percentageChange,
                forecastedMonthEndCostCents = comparison.forecastedMonthEndCostCents,
                forecastedVsPreviousPercentage = comparison.forecastedVsPreviousPercentage
            )
        )
    }
}
