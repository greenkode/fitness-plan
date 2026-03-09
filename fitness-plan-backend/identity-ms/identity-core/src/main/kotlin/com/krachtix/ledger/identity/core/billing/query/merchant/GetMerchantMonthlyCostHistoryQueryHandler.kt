package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.core.billing.dto.MerchantMonthlyCostHistoryResponse
import com.krachtix.identity.core.billing.dto.MerchantMonthlyCostResponse
import com.krachtix.identity.core.billing.service.BillingPlanService
import com.krachtix.identity.core.service.UserService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetMerchantMonthlyCostHistoryQueryHandler(
    private val billingDataGateway: BillingDataGateway,
    private val billingPlanService: BillingPlanService,
    private val userService: UserService
) : Command.Handler<GetMerchantMonthlyCostHistoryQuery, GetMerchantMonthlyCostHistoryResult> {

    override fun handle(query: GetMerchantMonthlyCostHistoryQuery): GetMerchantMonthlyCostHistoryResult {
        val merchantId = userService.getCurrentUser().organizationId
            ?: throw IllegalStateException("User has no organization")
        log.info { "Fetching monthly cost history for merchant=$merchantId, months=${query.months}" }

        val history = billingDataGateway.getCostHistory(merchantId, query.months)
        val plan = billingPlanService.getActivePlan(merchantId)

        return GetMerchantMonthlyCostHistoryResult(
            response = MerchantMonthlyCostHistoryResponse(
                history = history.map { cost ->
                    MerchantMonthlyCostResponse(
                        yearMonth = cost.yearMonth,
                        platformFeeCents = cost.platformFeeCents,
                        accountFeeCents = cost.accountFeeCents,
                        transactionFeeCents = cost.transactionFeeCents,
                        totalCents = cost.totalCents
                    )
                },
                currency = plan?.currency ?: "USD"
            )
        )
    }
}
