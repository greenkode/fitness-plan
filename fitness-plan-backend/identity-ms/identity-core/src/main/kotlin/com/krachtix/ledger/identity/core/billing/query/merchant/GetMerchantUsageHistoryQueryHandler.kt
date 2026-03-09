package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.core.billing.dto.MerchantUsageHistoryResponse
import com.krachtix.identity.core.billing.dto.MerchantUsageSnapshotResponse
import com.krachtix.identity.core.service.UserService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetMerchantUsageHistoryQueryHandler(
    private val billingDataGateway: BillingDataGateway,
    private val userService: UserService
) : Command.Handler<GetMerchantUsageHistoryQuery, GetMerchantUsageHistoryResult> {

    override fun handle(query: GetMerchantUsageHistoryQuery): GetMerchantUsageHistoryResult {
        val merchantId = userService.getCurrentUser().organizationId
            ?: throw IllegalStateException("User has no organization")
        log.info { "Fetching usage history for merchant=$merchantId, ${query.startDate} to ${query.endDate}" }

        val snapshots = billingDataGateway.getUsageHistory(merchantId, query.startDate, query.endDate)
        return GetMerchantUsageHistoryResult(
            response = MerchantUsageHistoryResponse(
                snapshots = snapshots.map { snapshot ->
                    MerchantUsageSnapshotResponse(
                        snapshotDate = snapshot.snapshotDate,
                        activeAccountCount = snapshot.activeAccountCount,
                        transactionCount = snapshot.transactionCount,
                        apiCallCount = snapshot.apiCallCount,
                        exportCount = snapshot.exportCount,
                        webhookDeliveryCount = snapshot.webhookDeliveryCount,
                        storageBytes = snapshot.storageBytes
                    )
                },
                startDate = query.startDate,
                endDate = query.endDate
            )
        )
    }
}
