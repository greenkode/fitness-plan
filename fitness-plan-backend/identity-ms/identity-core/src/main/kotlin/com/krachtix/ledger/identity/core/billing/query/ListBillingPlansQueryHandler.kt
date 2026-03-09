package com.krachtix.identity.core.billing.query

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.core.billing.dto.BillingPlanListResponse
import com.krachtix.identity.core.billing.dto.BillingPlanResponse
import com.krachtix.identity.core.billing.entity.BillingPlanEntity
import com.krachtix.identity.core.billing.service.BillingPlanService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class ListBillingPlansQueryHandler(
    private val billingPlanService: BillingPlanService
) : Command.Handler<ListBillingPlansQuery, ListBillingPlansResult> {

    override fun handle(query: ListBillingPlansQuery): ListBillingPlansResult {
        log.info { "Handling list billing plans query, organizationId=${query.organizationId}, status=${query.status}" }

        val pageable = PageRequest.of(query.page, query.size)
        val page = billingPlanService.listPlans(query.organizationId, query.status, pageable)

        val response = BillingPlanListResponse(
            plans = page.content.map { it.toResponse() },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )

        return ListBillingPlansResult(response = response)
    }

    private fun BillingPlanEntity.toResponse() = BillingPlanResponse(
        publicId = publicId,
        organizationId = organization.id,
        name = name,
        platformFeeAmount = platformFeeAmount,
        perAccountFeeAmount = perAccountFeeAmount,
        perTransactionFeeAmount = perTransactionFeeAmount,
        maxChargeAmount = maxChargeAmount,
        currency = currency,
        billingCycle = billingCycle,
        status = status,
        effectiveFrom = effectiveFrom,
        effectiveUntil = effectiveUntil,
        createdAt = createdAt
    )
}
