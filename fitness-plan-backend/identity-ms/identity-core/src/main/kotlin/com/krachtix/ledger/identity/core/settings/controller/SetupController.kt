package com.krachtix.identity.core.settings.controller

import com.krachtix.commons.dto.PhoneNumber
import com.krachtix.commons.security.IsMerchantSuperAdmin
import com.krachtix.identity.core.settings.command.CompleteSetupCommand
import com.krachtix.identity.core.settings.command.SaveBusinessProfileCommand
import com.krachtix.identity.core.settings.command.SaveCurrencySettingsCommand
import com.krachtix.identity.core.settings.command.SavePreferencesCommand
import com.krachtix.identity.core.settings.dto.CompleteSetupResponse
import com.krachtix.identity.core.settings.dto.SaveBusinessProfileRequest
import com.krachtix.identity.core.settings.dto.SaveBusinessProfileResponse
import com.krachtix.identity.core.settings.dto.SaveCurrencySettingsRequest
import com.krachtix.identity.core.settings.dto.SaveCurrencySettingsResponse
import com.krachtix.identity.core.settings.dto.SavePreferencesRequest
import com.krachtix.identity.core.settings.dto.SavePreferencesResponse
import com.krachtix.identity.core.settings.dto.SetupStatusResponse
import com.krachtix.identity.core.settings.query.GetSetupStatusQuery
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/merchant/setup")
@Tag(name = "Setup", description = "Organization setup wizard")
@IsMerchantSuperAdmin
class SetupController(
    private val pipeline: Pipeline
) {

    @GetMapping("/status")
    @Operation(summary = "Get setup status")
    @SecurityRequirement(name = "bearerAuth")
    fun getSetupStatus(): SetupStatusResponse {
        log.info { "Getting setup status" }

        val result = pipeline.send(GetSetupStatusQuery())

        return SetupStatusResponse(
            processId = result.processId,
            currentStep = result.currentStep,
            completedSteps = result.completedSteps,
            isComplete = result.isComplete,
            stepData = result.stepData
        )
    }

    @PostMapping("/profile")
    @Operation(summary = "Save business profile")
    @SecurityRequirement(name = "bearerAuth")
    fun saveBusinessProfile(@RequestBody request: SaveBusinessProfileRequest): SaveBusinessProfileResponse {
        log.info { "Saving business profile for company: ${request.companyName}" }

        val parsedPhoneNumber = request.phoneNumber.takeIf { it.isNotBlank() }?.let { raw ->
            val locale = Locale.Builder().setRegion(request.country).build()
            PhoneNumber.fromRawNumber(raw, locale)
        }

        val result = pipeline.send(
            SaveBusinessProfileCommand(
                companyName = request.companyName,
                intendedPurpose = request.intendedPurpose,
                companySize = request.companySize,
                roleInCompany = request.roleInCompany,
                country = request.country,
                phoneNumber = request.phoneNumber,
                parsedPhoneNumber = parsedPhoneNumber,
                website = request.website,
                termsAccepted = request.termsAccepted
            )
        )

        return SaveBusinessProfileResponse(
            success = result.success,
            message = result.message
        )
    }

    @PostMapping("/currency")
    @Operation(summary = "Save currency settings")
    @SecurityRequirement(name = "bearerAuth")
    fun saveCurrencySettings(@RequestBody request: SaveCurrencySettingsRequest): SaveCurrencySettingsResponse {
        log.info { "Saving currency settings: primary=${request.primaryCurrency}, multiCurrency=${request.multiCurrencyEnabled}" }

        val result = pipeline.send(
            SaveCurrencySettingsCommand(
                primaryCurrency = request.primaryCurrency,
                multiCurrencyEnabled = request.multiCurrencyEnabled,
                additionalCurrencies = request.additionalCurrencies,
                chartTemplateId = request.chartTemplateId,
                fiscalYearStart = request.fiscalYearStart
            )
        )

        return SaveCurrencySettingsResponse(
            success = result.success,
            message = result.message
        )
    }

    @PostMapping("/preferences")
    @Operation(summary = "Save preferences")
    @SecurityRequirement(name = "bearerAuth")
    fun savePreferences(@RequestBody request: SavePreferencesRequest): SavePreferencesResponse {
        log.info { "Saving preferences: timezone=${request.timezone}" }

        val result = pipeline.send(
            SavePreferencesCommand(
                timezone = request.timezone,
                dateFormat = request.dateFormat,
                numberFormat = request.numberFormat
            )
        )

        return SavePreferencesResponse(
            success = result.success,
            message = result.message
        )
    }

    @PostMapping("/complete")
    @Operation(summary = "Complete organization setup")
    @SecurityRequirement(name = "bearerAuth")
    fun completeSetup(): CompleteSetupResponse {
        log.info { "Completing organization setup" }

        val result = pipeline.send(CompleteSetupCommand())

        return CompleteSetupResponse(
            success = result.success,
            message = result.message,
            merchantId = result.merchantId
        )
    }
}
