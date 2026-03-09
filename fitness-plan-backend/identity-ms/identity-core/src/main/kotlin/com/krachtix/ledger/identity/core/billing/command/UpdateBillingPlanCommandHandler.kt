package com.krachtix.identity.core.billing.command

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.core.billing.dto.UpdateBillingPlanRequest
import com.krachtix.identity.core.billing.service.BillingPlanService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class UpdateBillingPlanCommandHandler(
    private val billingPlanService: BillingPlanService
) : Command.Handler<UpdateBillingPlanCommand, UpdateBillingPlanResult> {

    override fun handle(command: UpdateBillingPlanCommand): UpdateBillingPlanResult {
        log.info { "Handling update billing plan command for: ${command.publicId}" }

        val request = UpdateBillingPlanRequest(
            name = command.name,
            platformFeeAmount = command.platformFeeAmount,
            perAccountFeeAmount = command.perAccountFeeAmount,
            perTransactionFeeAmount = command.perTransactionFeeAmount,
            maxChargeAmount = command.maxChargeAmount,
            currency = command.currency,
            billingCycle = command.billingCycle,
            effectiveUntil = command.effectiveUntil
        )

        val plan = billingPlanService.updatePlan(command.publicId, request)

        return UpdateBillingPlanResult(
            publicId = plan.publicId,
            name = plan.name,
            status = plan.status.name
        )
    }
}
