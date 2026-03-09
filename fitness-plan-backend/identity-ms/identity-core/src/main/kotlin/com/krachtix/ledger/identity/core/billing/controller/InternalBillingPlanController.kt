package com.krachtix.identity.core.billing.controller

import io.swagger.v3.oas.annotations.tags.Tag
import com.krachtix.identity.core.billing.dto.ActivePlanWithOrgResponse
import com.krachtix.identity.core.billing.dto.BillingPlanInternalResponse
import com.krachtix.identity.core.billing.service.BillingPlanService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/internal/billing")
@Tag(name = "Internal Billing", description = "Internal billing endpoints for service-to-service communication")
class InternalBillingPlanController(
    private val billingPlanService: BillingPlanService
) {

    @GetMapping("/plans/active")
    fun getActivePlan(@RequestParam organizationId: UUID): BillingPlanInternalResponse? =
        billingPlanService.getActivePlanInternal(organizationId)

    @GetMapping("/plans/active/all")
    fun getAllActivePlansWithMerchant(): List<ActivePlanWithOrgResponse> =
        billingPlanService.getAllActivePlansWithMerchant()
}
