package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.core.billing.dto.MerchantBillingProjectionResponse
import com.krachtix.identity.core.service.UserService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetMerchantBillingProjectionQueryHandler(
    private val billingDataGateway: BillingDataGateway,
    private val userService: UserService
) : Command.Handler<GetMerchantBillingProjectionQuery, GetMerchantBillingProjectionResult> {

    override fun handle(query: GetMerchantBillingProjectionQuery): GetMerchantBillingProjectionResult {
        val merchantId = userService.getCurrentUser().organizationId
            ?: throw IllegalStateException("User has no organization")
        log.info { "Fetching billing projection for merchant=$merchantId" }

        val projection = billingDataGateway.getProjection(merchantId)
        return GetMerchantBillingProjectionResult(
            projection = MerchantBillingProjectionResponse(
                projectedAccountFeeCents = projection.projectedAccountFeeCents,
                projectedTransactionFeeCents = projection.projectedTransactionFeeCents,
                platformFeeCents = projection.platformFeeCents,
                projectedTotalCents = projection.projectedTotalCents
            )
        )
    }
}
