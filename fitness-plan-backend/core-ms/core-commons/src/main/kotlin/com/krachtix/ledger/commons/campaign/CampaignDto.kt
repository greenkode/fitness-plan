package com.krachtix.commons.campaign

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CampaignDto(
    val publicId: UUID,
    val name: String,
    val type: CampaignType,
    val value: BigDecimal,
    val minPurchaseAmount: BigDecimal?,
    val maxRedemptionAmount: BigDecimal?,
    val maxTotalRedemptions: Int?,
    val maxRedemptionsPerCustomer: Int?,
    val startDate: Instant,
    val endDate: Instant?,
    val status: CampaignStatus,
    val redemptionCount: Int,
    val currency: String,
    val isMerchantWide: Boolean,
    val merchantId: UUID,
    val accountProfileId: UUID?,
    val transactionGroup: String?,
    val transactionTemplateId: UUID?,
    val rewardExpiryDays: Int? = null,
    val pointsExchangeRate: BigDecimal? = null
)

data class CreateCampaignInput(
    val name: String,
    val type: CampaignType,
    val value: BigDecimal,
    val minPurchaseAmount: BigDecimal?,
    val maxRedemptionAmount: BigDecimal?,
    val maxTotalRedemptions: Int?,
    val maxRedemptionsPerCustomer: Int?,
    val startDate: Instant,
    val endDate: Instant?,
    val currencyCode: String,
    val isMerchantWide: Boolean,
    val eligibleCustomerIds: List<UUID>?,
    val accountProfileId: UUID?,
    val transactionGroup: String?,
    val transactionTemplateId: UUID?,
    val merchantId: UUID,
    val rewardExpiryDays: Int? = null,
    val pointsExchangeRate: BigDecimal? = null
)

data class UpdateCampaignInput(
    val name: String?,
    val value: BigDecimal?,
    val minPurchaseAmount: BigDecimal?,
    val maxRedemptionAmount: BigDecimal?,
    val maxTotalRedemptions: Int?,
    val maxRedemptionsPerCustomer: Int?,
    val startDate: Instant?,
    val endDate: Instant?,
    val accountProfileId: UUID?,
    val transactionGroup: String?,
    val transactionTemplateId: UUID?,
    val clearAccountProfileId: Boolean = false,
    val clearTransactionGroup: Boolean = false,
    val clearTransactionTemplateId: Boolean = false,
    val rewardExpiryDays: Int? = null,
    val clearRewardExpiryDays: Boolean = false,
    val pointsExchangeRate: BigDecimal? = null,
    val clearPointsExchangeRate: Boolean = false
)

data class CampaignRedemptionDto(
    val publicId: UUID,
    val customerId: UUID,
    val transactionReference: UUID,
    val originalAmount: BigDecimal,
    val campaignAmount: BigDecimal,
    val currency: String,
    val redeemedAt: Instant,
    val status: String = RedemptionStatus.ACTIVE.name,
    val expiresAt: Instant? = null,
    val rewardTransactionReference: UUID? = null
)

data class CampaignRedemptionPageDto(
    val redemptions: List<CampaignRedemptionDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int
)
