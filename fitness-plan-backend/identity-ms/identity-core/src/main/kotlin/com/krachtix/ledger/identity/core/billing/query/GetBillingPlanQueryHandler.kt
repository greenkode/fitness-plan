package com.krachtix.identity.core.billing.query

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.billing.dto.BillingPlanResponse
import com.krachtix.identity.core.billing.entity.BillingPlanEntity
import com.krachtix.identity.core.billing.service.BillingPlanService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetBillingPlanQueryHandler(
    private val billingPlanService: BillingPlanService,
    private val messageService: MessageService
) : Command.Handler<GetBillingPlanQuery, GetBillingPlanResult> {

    override fun handle(query: GetBillingPlanQuery): GetBillingPlanResult {
        log.info { "Handling get billing plan query for: ${query.publicId}" }

        val plan = billingPlanService.getPlan(query.publicId)
            ?: throw RecordNotFoundException(messageService.getMessage("billing.error.plan_not_found"))

        return GetBillingPlanResult(plan = plan.toResponse())
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
