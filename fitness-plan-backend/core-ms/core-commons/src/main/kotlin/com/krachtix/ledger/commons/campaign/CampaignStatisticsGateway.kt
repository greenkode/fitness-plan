package com.krachtix.commons.campaign

import com.krachtix.commons.customer.DailyVolumeDto
import java.time.Instant

interface CampaignStatisticsGateway {

    fun getRedemptionStats(): RedemptionStatsDto

    fun getDailyRedemptionVolume(startDate: Instant, endDate: Instant): List<DailyVolumeDto>
}

data class RedemptionStatsDto(
    val totalRedemptions: Long,
    val activeRedemptions: Long
)
