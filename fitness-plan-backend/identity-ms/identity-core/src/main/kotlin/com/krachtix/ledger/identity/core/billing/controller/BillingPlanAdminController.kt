package com.krachtix.identity.core.billing.controller

import an.awesome.pipelinr.Pipeline
import io.swagger.v3.oas.annotations.tags.Tag
import com.krachtix.identity.core.billing.command.CreateBillingPlanCommand
import com.krachtix.identity.core.billing.command.DeactivateBillingPlanCommand
import com.krachtix.identity.core.billing.command.UpdateBillingPlanCommand
import com.krachtix.identity.core.billing.dto.BillingPlanListResponse
import com.krachtix.identity.core.billing.dto.BillingPlanResponse
import com.krachtix.identity.core.billing.dto.CreateBillingPlanRequest
import com.krachtix.identity.core.billing.dto.UpdateBillingPlanRequest
import com.krachtix.identity.core.billing.entity.BillingPlanStatus
import com.krachtix.identity.core.billing.query.GetBillingPlanQuery
import com.krachtix.identity.core.billing.query.ListBillingPlansQuery
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/admin/billing")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Billing Plan Admin", description = "Admin operations for billing plan management")
class BillingPlanAdminController(
    private val pipeline: Pipeline
) {

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPlan(@RequestBody request: CreateBillingPlanRequest): BillingPlanResponse {
        val result = pipeline.send(
            CreateBillingPlanCommand(
                organizationId = request.organizationId,
                name = request.name,
                platformFeeAmount = request.platformFeeAmount,
                perAccountFeeAmount = request.perAccountFeeAmount,
                perTransactionFeeAmount = request.perTransactionFeeAmount,
                maxChargeAmount = request.maxChargeAmount,
                currency = request.currency,
                billingCycle = request.billingCycle,
                effectiveFrom = request.effectiveFrom,
                effectiveUntil = request.effectiveUntil
            )
        )

        val planResult = pipeline.send(GetBillingPlanQuery(publicId = result.publicId))
        return planResult.plan
    }

    @PutMapping("/plans/{publicId}")
    fun updatePlan(
        @PathVariable publicId: UUID,
        @RequestBody request: UpdateBillingPlanRequest
    ): BillingPlanResponse {
        pipeline.send(
            UpdateBillingPlanCommand(
                publicId = publicId,
                name = request.name,
                platformFeeAmount = request.platformFeeAmount,
                perAccountFeeAmount = request.perAccountFeeAmount,
                perTransactionFeeAmount = request.perTransactionFeeAmount,
                maxChargeAmount = request.maxChargeAmount,
                currency = request.currency,
                billingCycle = request.billingCycle,
                effectiveUntil = request.effectiveUntil
            )
        )

        val planResult = pipeline.send(GetBillingPlanQuery(publicId = publicId))
        return planResult.plan
    }

    @DeleteMapping("/plans/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivatePlan(@PathVariable publicId: UUID) {
        pipeline.send(DeactivateBillingPlanCommand(publicId = publicId))
    }

    @GetMapping("/plans")
    fun listPlans(
        @RequestParam(required = false) organizationId: UUID?,
        @RequestParam(required = false) status: BillingPlanStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): BillingPlanListResponse {
        val result = pipeline.send(
            ListBillingPlansQuery(
                organizationId = organizationId,
                status = status,
                page = page,
                size = size
            )
        )
        return result.response
    }

    @GetMapping("/plans/{publicId}")
    fun getPlan(@PathVariable publicId: UUID): BillingPlanResponse {
        val result = pipeline.send(GetBillingPlanQuery(publicId = publicId))
        return result.plan
    }
}
