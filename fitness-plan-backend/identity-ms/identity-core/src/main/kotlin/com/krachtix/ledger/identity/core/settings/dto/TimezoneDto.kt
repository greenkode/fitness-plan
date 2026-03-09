package com.krachtix.identity.core.settings.dto

data class TimezoneResponse(
    val id: String,
    val displayName: String,
    val offset: String
)

data class TimezoneGroupResponse(
    val region: String,
    val timezones: List<TimezoneResponse>
)

data class DateFormatOption(
    val value: String,
    val label: String,
    val example: String
)

data class NumberFormatOption(
    val value: String,
    val label: String,
    val example: String
)
