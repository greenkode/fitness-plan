package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.core.billing.dto.MerchantBillingPlanResponse
import com.krachtix.identity.core.billing.service.BillingPlanService
import com.krachtix.identity.core.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetMerchantBillingPlanQueryHandler(
    private val billingPlanService: BillingPlanService,
    private val userService: UserService
) : Command.Handler<GetMerchantBillingPlanQuery, GetMerchantBillingPlanResult> {

    override fun handle(query: GetMerchantBillingPlanQuery): GetMerchantBillingPlanResult {
        val merchantId = userService.getCurrentUser().organizationId
            ?: throw IllegalStateException("User has no organization")
        log.info { "Fetching billing plan for merchant=$merchantId" }

        val plan = billingPlanService.getActivePlan(merchantId)
        return GetMerchantBillingPlanResult(
            plan = plan?.let {
                MerchantBillingPlanResponse(
                    publicId = it.publicId,
                    name = it.name,
                    platformFeeAmount = it.platformFeeAmount,
                    perAccountFeeAmount = it.perAccountFeeAmount,
                    perTransactionFeeAmount = it.perTransactionFeeAmount,
                    maxChargeAmount = it.maxChargeAmount,
                    currency = it.currency,
                    billingCycle = it.billingCycle.name,
                    status = it.status.name,
                    effectiveFrom = it.effectiveFrom,
                    effectiveUntil = it.effectiveUntil
                )
            }
        )
    }
}
