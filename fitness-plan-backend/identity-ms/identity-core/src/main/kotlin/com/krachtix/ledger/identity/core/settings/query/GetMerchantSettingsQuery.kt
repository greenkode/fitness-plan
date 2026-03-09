package com.krachtix.identity.core.settings.query

import an.awesome.pipelinr.Command
import java.util.UUID

data class GetMerchantSettingsQuery(
    val merchantId: UUID
) : Command<GetMerchantSettingsResult>

data class GetMerchantSettingsResult(
    val defaultCurrency: String,
    val additionalCurrencies: List<String>,
    val chartTemplateId: String,
    val setupCompleted: Boolean,
    val timezone: String?,
    val organizationName: String
)
