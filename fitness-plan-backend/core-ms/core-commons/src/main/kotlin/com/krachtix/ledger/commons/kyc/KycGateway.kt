package com.krachtix.commons.kyc

import com.krachtix.commons.kyc.dto.BvnData
import com.krachtix.kyc.dto.RequiredFieldAction
import com.krachtix.kyc.dto.RequiredKycFieldDto
import com.krachtix.commons.dto.UserType
import com.krachtix.kyc.VerificationStatus
import java.util.UUID

interface KycGateway {

    fun getRequiredKycFields(action: RequiredFieldAction, userType: UserType): List<RequiredKycFieldDto>

    fun getVerificationTier(verificationDetails: Map<String, VerificationStatus>): TrustLevel

    fun getBvnDetails(bvn: String, publicId: UUID) : BvnData?
}