package com.krachtix.identity.core.billing.command

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.core.billing.service.BillingPlanService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class DeactivateBillingPlanCommandHandler(
    private val billingPlanService: BillingPlanService
) : Command.Handler<DeactivateBillingPlanCommand, DeactivateBillingPlanResult> {

    override fun handle(command: DeactivateBillingPlanCommand): DeactivateBillingPlanResult {
        log.info { "Handling deactivate billing plan command for: ${command.publicId}" }

        val plan = billingPlanService.deactivatePlan(command.publicId)

        return DeactivateBillingPlanResult(
            publicId = plan.publicId,
            status = plan.status.name
        )
    }
}
