package com.krachtix.identity.core.settings.command

import an.awesome.pipelinr.Command

data class SavePreferencesCommand(
    val timezone: String,
    val dateFormat: String,
    val numberFormat: String
) : Command<SavePreferencesResult>

data class SavePreferencesResult(
    val success: Boolean,
    val message: String
)
