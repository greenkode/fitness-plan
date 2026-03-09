package com.krachtix.identity.core.settings.dto

data class UserPreferencesRequest(
    val timezone: String?,
    val dateFormat: String?,
    val numberFormat: String?
)

data class UserPreferencesResponse(
    val timezone: String?,
    val dateFormat: String?,
    val numberFormat: String?
)

data class EffectiveLocaleResponse(
    val timezone: String,
    val dateFormat: String,
    val numberFormat: String,
    val source: LocaleSource
)

data class LocaleSource(
    val timezone: String,
    val dateFormat: String,
    val numberFormat: String
)
