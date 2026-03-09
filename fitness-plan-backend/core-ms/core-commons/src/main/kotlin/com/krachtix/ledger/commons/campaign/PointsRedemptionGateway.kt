package com.krachtix.commons.campaign

import java.math.BigDecimal
import java.util.UUID
import javax.money.MonetaryAmount

interface PointsRedemptionGateway {

    fun getPointsBalance(customerAccountPublicId: UUID): BigDecimal

    fun redeemPoints(
        campaignPublicId: UUID,
        customerAccountPublicId: UUID,
        pointsAmount: BigDecimal,
        initiatedBy: String
    ): PointsRedemptionResult
}

data class PointsRedemptionResult(
    val transactionReference: UUID,
    val displayRef: String,
    val pointsRedeemed: BigDecimal,
    val monetaryAmount: MonetaryAmount
)
