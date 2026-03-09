package com.krachtix.identity.core.billing.command

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.core.billing.dto.CreateBillingPlanRequest
import com.krachtix.identity.core.billing.service.BillingPlanService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class CreateBillingPlanCommandHandler(
    private val billingPlanService: BillingPlanService
) : Command.Handler<CreateBillingPlanCommand, CreateBillingPlanResult> {

    override fun handle(command: CreateBillingPlanCommand): CreateBillingPlanResult {
        log.info { "Handling create billing plan command for organization: ${command.organizationId}" }

        val request = CreateBillingPlanRequest(
            organizationId = command.organizationId,
            name = command.name,
            platformFeeAmount = command.platformFeeAmount,
            perAccountFeeAmount = command.perAccountFeeAmount,
            perTransactionFeeAmount = command.perTransactionFeeAmount,
            maxChargeAmount = command.maxChargeAmount,
            currency = command.currency,
            billingCycle = command.billingCycle,
            effectiveFrom = command.effectiveFrom,
            effectiveUntil = command.effectiveUntil
        )

        val plan = billingPlanService.createPlan(request)

        return CreateBillingPlanResult(
            publicId = plan.publicId,
            organizationId = plan.organization.id,
            name = plan.name,
            status = plan.status.name
        )
    }
}
