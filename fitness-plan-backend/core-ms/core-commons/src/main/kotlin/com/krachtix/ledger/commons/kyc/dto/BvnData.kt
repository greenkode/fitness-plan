package com.krachtix.commons.kyc.dto


import com.krachtix.commons.dto.PhoneNumber
import java.time.LocalDate

data class BvnData(
    val bvn: String, val firstName: String, val middleName: String,
    val lastName: String, val dateOfBirth: LocalDate,
    val watchListed: Boolean, val address: String, val phoneNumbers: Set<PhoneNumber>
) 