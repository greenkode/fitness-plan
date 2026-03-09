package com.krachtix.identity.core.settings.command

import an.awesome.pipelinr.Command

data class SaveCurrencySettingsCommand(
    val primaryCurrency: String,
    val multiCurrencyEnabled: Boolean,
    val additionalCurrencies: List<String>,
    val chartTemplateId: String,
    val fiscalYearStart: String
) : Command<SaveCurrencySettingsResult>

data class SaveCurrencySettingsResult(
    val success: Boolean,
    val message: String
)
