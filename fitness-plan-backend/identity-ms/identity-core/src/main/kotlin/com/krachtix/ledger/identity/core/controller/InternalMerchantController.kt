package com.krachtix.identity.core.controller

import an.awesome.pipelinr.Pipeline
import com.krachtix.identity.commons.dto.UpdateMerchantEnvironmentResponse
import com.krachtix.identity.core.entity.EnvironmentMode
import com.krachtix.identity.core.currency.service.CurrencyLimitService
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.service.MerchantService
import com.krachtix.identity.core.settings.query.GetMerchantSettingsQuery
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/internal/merchants")
class InternalMerchantController(
    private val merchantService: MerchantService,
    private val pipeline: Pipeline,
    private val organizationRepository: OrganizationRepository,
    private val currencyLimitService: CurrencyLimitService
) {

    @PutMapping("/{merchantId}/environment")
    fun updateMerchantEnvironment(
        @PathVariable merchantId: String,
        @RequestBody request: UpdateMerchantEnvironmentRequest
    ): UpdateMerchantEnvironmentResponse {
        log.info { "Internal request to update merchant environment for: $merchantId" }

        return merchantService.updateMerchantEnvironment(UUID.fromString(merchantId), request.environmentMode)
            .let { result ->
                UpdateMerchantEnvironmentResponse(
                    merchantId = result.merchantId,
                    environmentMode = result.environmentMode.name,
                    lastModifiedAt = result.lastModifiedAt,
                    affectedUsers = result.affectedUsers
                )
            }
    }

    @GetMapping("/{merchantId}/settings")
    @PreAuthorize("hasAuthority('SCOPE_internal:read')")
    fun getMerchantSettings(@PathVariable merchantId: UUID): MerchantSettingsResponse {
        log.info { "Internal request to get merchant settings for: $merchantId" }

        val result = pipeline.send(GetMerchantSettingsQuery(merchantId = merchantId))

        val organization = organizationRepository.findById(merchantId).orElse(null)
        val subscriptionTier = organization?.plan?.name ?: "TRIAL"
        val maxCurrencies = organization?.let { currencyLimitService.getMaxCurrencies(it.plan) } ?: 1

        val response = MerchantSettingsResponse(
            defaultCurrency = result.defaultCurrency,
            additionalCurrencies = result.additionalCurrencies,
            chartTemplateId = result.chartTemplateId,
            setupCompleted = result.setupCompleted,
            subscriptionTier = subscriptionTier,
            maxCurrencies = maxCurrencies,
            timezone = result.timezone,
            organizationName = result.organizationName
        )

        log.info { "Returning merchant settings response: $response" }

        return response
    }
}

data class UpdateMerchantEnvironmentRequest(
    @JsonProperty("environmentMode")
    val environmentMode: EnvironmentMode
)

data class MerchantSettingsResponse(
    val defaultCurrency: String?,
    val additionalCurrencies: List<String>,
    val chartTemplateId: String?,
    val setupCompleted: Boolean,
    val subscriptionTier: String,
    val maxCurrencies: Int,
    val timezone: String?,
    val organizationName: String
)
