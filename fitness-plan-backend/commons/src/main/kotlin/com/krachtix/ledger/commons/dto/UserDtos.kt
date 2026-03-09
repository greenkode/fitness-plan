package com.krachtix.commons.dto

import com.neovisionaries.i18n.CountryCode
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.kyc.TrustLevel
import com.krachtix.user.dto.UserExternalId
import com.krachtix.user.dto.UserProperty
import org.apache.commons.validator.routines.EmailValidator
import java.io.Serializable
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class MerchantDetailsDto(
    val id: UUID,
    val name: String,
    val email: String,
    val phoneNumber: String? = null,
    val locale: Locale
) : Serializable

data class UserDetailsDto(
    val publicId: UUID,
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
    val userProperties: MutableSet<UserProperty> = mutableSetOf()
) : Serializable

data class Email(val value: String) : Serializable {
    init {
        if(!EmailValidator.getInstance().isValid(value))
            throw InvalidRequestException("Email address $value is not valid")
    }
}

enum class UserType {
    INDIVIDUAL,
    BUSINESS,
    SYSTEM
}

data class UserAddress(
    val country: CountryCode,
    val state: String,
    val zipCode: String,
    val city: String,
    val street: String,
    val number: String?
) : Serializable {
    override fun toString() = "$number, $street, $city, $state, $zipCode, $country"
}

