package com.krachtix.commons.billing

import java.util.UUID

interface MerchantRestrictionGateway {
    fun isMerchantRestricted(merchantId: UUID): Boolean
    fun restrictMerchant(merchantId: UUID, reason: String)
    fun liftRestriction(merchantId: UUID)
}

data class MerchantRestrictionDto(
    val restricted: Boolean,
    val reason: String?
)
