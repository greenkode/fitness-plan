package com.krachtix.kyc.dto

import com.krachtix.kyc.VerificationStatus

data class RequiredKycFieldDto(val name: String, val status: VerificationStatus, val required: Boolean)
