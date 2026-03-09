package com.krachtix.identity.core.settings.controller

import com.krachtix.commons.security.IsMerchant
import com.krachtix.identity.core.entity.OAuthUserSettingName
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.settings.dto.EffectiveLocaleResponse
import com.krachtix.identity.core.settings.dto.LocaleSource
import com.krachtix.identity.core.settings.dto.TourStateResponse
import com.krachtix.identity.core.settings.dto.UserPreferencesRequest
import com.krachtix.identity.core.settings.dto.UserPreferencesResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/user/preferences")
@Tag(name = "User Preferences", description = "User locale and preference settings")
@IsMerchant
class UserPreferencesController(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val organizationRepository: OrganizationRepository
) {

    companion object {
        private const val MAX_TOUR_VIEWS = 1
    }

    @GetMapping
    @Operation(summary = "Get user preference overrides")
    @SecurityRequirement(name = "bearerAuth")
    @Transactional(readOnly = true)
    fun getUserPreferences(): UserPreferencesResponse {
        val user = userService.getCurrentUser()
        val userWithSettings = userRepository.findByIdWithSettings(user.id!!)
            ?: throw IllegalStateException("User not found")

        return UserPreferencesResponse(
            timezone = userWithSettings.getSetting(OAuthUserSettingName.TIMEZONE),
            dateFormat = userWithSettings.getSetting(OAuthUserSettingName.DATE_FORMAT),
            numberFormat = userWithSettings.getSetting(OAuthUserSettingName.NUMBER_FORMAT)
        )
    }

    @PutMapping
    @Operation(summary = "Update user preference overrides")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    fun updateUserPreferences(@RequestBody request: UserPreferencesRequest): UserPreferencesResponse {
        val user = userService.getCurrentUser()
        val userWithSettings = userRepository.findByIdWithSettings(user.id!!)
            ?: throw IllegalStateException("User not found")

        if (request.timezone != null) {
            userWithSettings.addSetting(OAuthUserSettingName.TIMEZONE, request.timezone)
        } else {
            userWithSettings.removeSetting(OAuthUserSettingName.TIMEZONE)
        }

        if (request.dateFormat != null) {
            userWithSettings.addSetting(OAuthUserSettingName.DATE_FORMAT, request.dateFormat)
        } else {
            userWithSettings.removeSetting(OAuthUserSettingName.DATE_FORMAT)
        }

        if (request.numberFormat != null) {
            userWithSettings.addSetting(OAuthUserSettingName.NUMBER_FORMAT, request.numberFormat)
        } else {
            userWithSettings.removeSetting(OAuthUserSettingName.NUMBER_FORMAT)
        }

        userRepository.save(userWithSettings)

        log.info { "Updated locale preferences for user: ${userWithSettings.id}" }

        return UserPreferencesResponse(
            timezone = userWithSettings.getSetting(OAuthUserSettingName.TIMEZONE),
            dateFormat = userWithSettings.getSetting(OAuthUserSettingName.DATE_FORMAT),
            numberFormat = userWithSettings.getSetting(OAuthUserSettingName.NUMBER_FORMAT)
        )
    }

    @GetMapping("/tour")
    @Operation(summary = "Get guided tour state")
    @SecurityRequirement(name = "bearerAuth")
    @Transactional(readOnly = true)
    fun getTourState(): TourStateResponse {
        val user = userService.getCurrentUser()
        val userWithSettings = userRepository.findByIdWithSettings(user.id!!)
            ?: throw IllegalStateException("User not found")

        val viewCount = userWithSettings.getSetting(OAuthUserSettingName.GUIDED_TOUR_VIEW_COUNT)
            ?.toIntOrNull() ?: 0

        return TourStateResponse(
            viewCount = viewCount,
            tourCompleted = viewCount >= MAX_TOUR_VIEWS
        )
    }

    @PostMapping("/tour/viewed")
    @Operation(summary = "Record a guided tour view")
    @SecurityRequirement(name = "bearerAuth")
    @Transactional
    fun recordTourView(): TourStateResponse {
        val user = userService.getCurrentUser()
        val userWithSettings = userRepository.findByIdWithSettings(user.id!!)
            ?: throw IllegalStateException("User not found")

        val currentCount = userWithSettings.getSetting(OAuthUserSettingName.GUIDED_TOUR_VIEW_COUNT)
            ?.toIntOrNull() ?: 0
        val newCount = currentCount + 1

        userWithSettings.addSetting(OAuthUserSettingName.GUIDED_TOUR_VIEW_COUNT, newCount.toString())
        userRepository.save(userWithSettings)

        log.info { "Recorded tour view for user: ${userWithSettings.id}, count: $newCount" }

        return TourStateResponse(
            viewCount = newCount,
            tourCompleted = newCount >= MAX_TOUR_VIEWS
        )
    }

    @GetMapping("/effective")
    @Operation(summary = "Get effective locale settings (user override or organization default)")
    @SecurityRequirement(name = "bearerAuth")
    @Transactional(readOnly = true)
    fun getEffectiveLocale(): EffectiveLocaleResponse {
        val user = userService.getCurrentUser()
        val userWithSettings = userRepository.findByIdWithSettings(user.id!!)
            ?: throw IllegalStateException("User not found")

        val merchantId = user.merchantId
        val organization = merchantId?.let { organizationRepository.findByIdWithProperties(it).orElse(null) }

        val orgTimezone = organization?.getProperty(OrganizationPropertyName.TIMEZONE) ?: "UTC"
        val orgDateFormat = organization?.getProperty(OrganizationPropertyName.DATE_FORMAT) ?: "YYYY-MM-DD"
        val orgNumberFormat = organization?.getProperty(OrganizationPropertyName.NUMBER_FORMAT) ?: "1,234.56"

        val userTimezone = userWithSettings.getSetting(OAuthUserSettingName.TIMEZONE)
        val userDateFormat = userWithSettings.getSetting(OAuthUserSettingName.DATE_FORMAT)
        val userNumberFormat = userWithSettings.getSetting(OAuthUserSettingName.NUMBER_FORMAT)

        val effectiveTimezone = userTimezone ?: orgTimezone
        val effectiveDateFormat = userDateFormat ?: orgDateFormat
        val effectiveNumberFormat = userNumberFormat ?: orgNumberFormat

        return EffectiveLocaleResponse(
            timezone = effectiveTimezone,
            dateFormat = effectiveDateFormat,
            numberFormat = effectiveNumberFormat,
            source = LocaleSource(
                timezone = if (userTimezone != null) "user" else "organization",
                dateFormat = if (userDateFormat != null) "user" else "organization",
                numberFormat = if (userNumberFormat != null) "user" else "organization"
            )
        )
    }
}
