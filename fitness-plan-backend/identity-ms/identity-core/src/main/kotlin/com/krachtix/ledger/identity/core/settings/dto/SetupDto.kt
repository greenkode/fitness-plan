package com.krachtix.identity.core.settings.dto

import com.krachtix.identity.core.entity.CompanyRole
import com.krachtix.identity.core.entity.CompanySize
import com.krachtix.identity.core.entity.IntendedPurpose
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Setup status response")
data class SetupStatusResponse(
    val processId: UUID?,
    val currentStep: Int,
    val completedSteps: List<String>,
    val isComplete: Boolean,
    val stepData: Map<String, Map<String, String>> = emptyMap()
)

@Schema(description = "Save business profile request")
data class SaveBusinessProfileRequest(
    val companyName: String,
    val intendedPurpose: IntendedPurpose,
    val companySize: CompanySize,
    val roleInCompany: CompanyRole,
    val country: String,
    val phoneNumber: String,
    val website: String?,
    val termsAccepted: Boolean
)

@Schema(description = "Save business profile response")
data class SaveBusinessProfileResponse(
    val success: Boolean,
    val message: String
)

@Schema(description = "Complete setup response")
data class CompleteSetupResponse(
    val success: Boolean,
    val message: String,
    val merchantId: String? = null
)

@Schema(description = "Save currency settings request")
data class SaveCurrencySettingsRequest(
    @Schema(description = "Primary currency ISO code", example = "USD")
    val primaryCurrency: String,
    @Schema(description = "Whether multi-currency mode is enabled", example = "false")
    val multiCurrencyEnabled: Boolean = false,
    @Schema(description = "Additional currency ISO codes for multi-currency mode", example = "[\"EUR\", \"GBP\"]")
    val additionalCurrencies: List<String> = emptyList(),
    @Schema(description = "Chart of accounts template ID", example = "standard-general")
    val chartTemplateId: String,
    @Schema(description = "Fiscal year start month (01-12)", example = "01")
    val fiscalYearStart: String
)

@Schema(description = "Save currency settings response")
data class SaveCurrencySettingsResponse(
    val success: Boolean,
    val message: String
)

@Schema(description = "Save preferences request")
data class SavePreferencesRequest(
    @Schema(description = "Timezone", example = "America/New_York")
    val timezone: String,
    @Schema(description = "Date format preference", example = "MM/DD/YYYY")
    val dateFormat: String,
    @Schema(description = "Number format preference", example = "1,234.56")
    val numberFormat: String
)

@Schema(description = "Save preferences response")
data class SavePreferencesResponse(
    val success: Boolean,
    val message: String
)
