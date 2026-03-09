package com.krachtix.commons.user

import com.krachtix.commons.dto.Email
import com.krachtix.commons.dto.PhoneNumber
import com.krachtix.commons.dto.UserAddress
import com.krachtix.commons.dto.UserDetailsDto
import com.krachtix.commons.dto.UserType
import com.krachtix.commons.kyc.TrustLevel
import com.krachtix.user.dto.UserExternalId
import com.krachtix.user.dto.UserProperty
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val merchantId: UUID?,
    val trustLevel: TrustLevel,
    val type: UserType,
    val firstName: String?,
    val lastName: String?,
    val locale: Locale,
    val dateOfBirth: LocalDate? = null,
    val address: UserAddress? = null,
    val email: Email? = null,
    val phoneNumber: PhoneNumber? = null,
    val externalIds: MutableSet<UserExternalId> = mutableSetOf(),
    val properties: MutableSet<UserProperty> = mutableSetOf()
) {
    fun toDto(): UserDetailsDto {
        return UserDetailsDto(id, merchantId, trustLevel,
            type, firstName, lastName, locale, dateOfBirth,
            address, email, phoneNumber, externalIds, properties
            )
    }
}

object UserAttributes {
    const val PHONE_NUMBER = "phoneNumber"
    const val TYPE = "type"
    const val COUNTRY = "country"
    const val TAX_IDENTIFICATION_NUMBER = "taxIdentificationNumber"
    const val DATE_OF_BIRTH = "dob"
    const val STREET = "addressStreet"
    const val CITY = "addressCity"
    const val ADDRESS_COUNTRY = "addressCountry"
    const val STATE = "addressState"
    const val ZIP_CODE = "zipCode"
    const val ADDRESS_NUMBER = "addressNumber"

}