package com.krachtix.commons.merchant

import java.io.Serializable
import java.util.UUID

interface MerchantSettingsGateway {
    fun getMerchantSettings(merchantId: UUID): MerchantSettingsDto
}

data class MerchantSettingsDto(
    val defaultCurrency: String,
    val additionalCurrencies: List<String>,
    val chartTemplateId: UUID,
    val setupCompleted: Boolean,
    val subscriptionTier: String,
    val maxCurrencies: Int,
    val timezone: String?,
    val organizationName: String,
    val restricted: Boolean = false
) : Serializable
