package com.krachtix.commons.campaign

import java.util.UUID

interface CampaignGateway {

    fun findByPublicId(publicId: UUID): CampaignDto?

    fun findByMerchantId(merchantId: UUID): List<CampaignDto>

    fun findByMerchantIdAndStatus(merchantId: UUID, status: CampaignStatus): List<CampaignDto>

    fun createCampaign(input: CreateCampaignInput): CampaignDto

    fun updateCampaign(publicId: UUID, input: UpdateCampaignInput): CampaignDto

    fun deactivateCampaign(publicId: UUID)

    fun addEligibilities(campaignPublicId: UUID, customerIds: List<UUID>): Int

    fun getExistingEligibleCustomerIds(campaignPublicId: UUID, customerIds: List<UUID>): Set<UUID>

    fun countEligibilities(campaignPublicId: UUID): Int

    fun findRedemptions(campaignPublicId: UUID, page: Int, size: Int): CampaignRedemptionPageDto
}
