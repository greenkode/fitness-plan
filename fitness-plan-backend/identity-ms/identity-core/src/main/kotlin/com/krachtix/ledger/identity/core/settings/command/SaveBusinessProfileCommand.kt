package com.krachtix.identity.core.settings.command

import com.krachtix.commons.dto.PhoneNumber
import com.krachtix.identity.core.entity.CompanyRole
import com.krachtix.identity.core.entity.CompanySize
import com.krachtix.identity.core.entity.IntendedPurpose
import an.awesome.pipelinr.Command

data class SaveBusinessProfileCommand(
    val companyName: String,
    val intendedPurpose: IntendedPurpose,
    val companySize: CompanySize,
    val roleInCompany: CompanyRole,
    val country: String,
    val phoneNumber: String,
    val parsedPhoneNumber: PhoneNumber?,
    val website: String?,
    val termsAccepted: Boolean
) : Command<SaveBusinessProfileResult>

data class SaveBusinessProfileResult(
    val success: Boolean,
    val message: String
)
