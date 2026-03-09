package com.krachtix.identity.core.billing.controller

import an.awesome.pipelinr.Pipeline
import io.swagger.v3.oas.annotations.tags.Tag
import com.krachtix.commons.security.IsMerchant
import com.krachtix.identity.core.billing.dto.MerchantBillingPlanResponse
import com.krachtix.identity.core.billing.dto.MerchantBillingProjectionResponse
import com.krachtix.identity.core.billing.dto.MerchantCostComparisonResponse
import com.krachtix.identity.core.billing.dto.MerchantCurrentUsageResponse
import com.krachtix.identity.core.billing.dto.MerchantInvoiceListResponse
import com.krachtix.identity.core.billing.dto.MerchantInvoiceResponse
import com.krachtix.identity.core.billing.dto.MerchantMonthlyCostHistoryResponse
import com.krachtix.identity.core.billing.dto.MerchantPaymentUrlResponse
import com.krachtix.identity.core.billing.dto.MerchantRestrictionStatusResponse
import com.krachtix.identity.core.billing.dto.MerchantUsageHistoryResponse
import com.krachtix.identity.core.billing.query.merchant.GetMerchantBillingPlanQuery
import com.krachtix.identity.core.billing.query.merchant.GetMerchantBillingProjectionQuery
import com.krachtix.identity.core.billing.query.merchant.GetMerchantCostComparisonQuery
import com.krachtix.identity.core.billing.query.merchant.GetMerchantCurrentUsageQuery
import com.krachtix.identity.core.billing.query.merchant.GetMerchantInvoicePaymentUrlQuery
import com.krachtix.identity.core.billing.query.merchant.GetMerchantInvoiceQuery
import com.krachtix.identity.core.billing.query.merchant.GetMerchantInvoicesQuery
import com.krachtix.identity.core.billing.query.merchant.GetMerchantMonthlyCostHistoryQuery
import com.krachtix.identity.core.billing.query.merchant.GetMerchantRestrictionStatusQuery
import com.krachtix.identity.core.billing.query.merchant.GetMerchantUsageHistoryQuery
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@IsMerchant
@RestController
@RequestMapping("/merchant/billing")
@Tag(name = "Merchant Billing")
class MerchantBillingController(
    private val pipeline: Pipeline
) {

    @GetMapping("/plan")
    fun getPlan(): MerchantBillingPlanResponse? =
        pipeline.send(GetMerchantBillingPlanQuery()).plan

    @GetMapping("/usage")
    fun getCurrentUsage(): MerchantCurrentUsageResponse =
        pipeline.send(GetMerchantCurrentUsageQuery()).usage

    @GetMapping("/projection")
    fun getProjection(): MerchantBillingProjectionResponse =
        pipeline.send(GetMerchantBillingProjectionQuery()).projection

    @GetMapping("/restriction-status")
    fun getRestrictionStatus(): MerchantRestrictionStatusResponse =
        pipeline.send(GetMerchantRestrictionStatusQuery()).response

    @GetMapping("/invoices/{publicId}/payment-url")
    fun getInvoicePaymentUrl(@PathVariable publicId: UUID): MerchantPaymentUrlResponse =
        pipeline.send(GetMerchantInvoicePaymentUrlQuery(publicId = publicId)).response

    @GetMapping("/invoices")
    fun getInvoices(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: String?
    ): MerchantInvoiceListResponse =
        pipeline.send(GetMerchantInvoicesQuery(page = page, size = size, status = status)).response

    @GetMapping("/invoices/{publicId}")
    fun getInvoice(@PathVariable publicId: UUID): MerchantInvoiceResponse =
        pipeline.send(GetMerchantInvoiceQuery(publicId = publicId)).invoice

    @GetMapping("/cost-history")
    fun getCostHistory(@RequestParam(defaultValue = "6") months: Int): MerchantMonthlyCostHistoryResponse =
        pipeline.send(GetMerchantMonthlyCostHistoryQuery(months = months)).response

    @GetMapping("/cost-comparison")
    fun getCostComparison(): MerchantCostComparisonResponse =
        pipeline.send(GetMerchantCostComparisonQuery()).response

    @GetMapping("/usage-history")
    fun getUsageHistory(
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate
    ): MerchantUsageHistoryResponse =
        pipeline.send(GetMerchantUsageHistoryQuery(startDate = startDate, endDate = endDate)).response
}
