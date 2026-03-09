package com.krachtix.identity.core.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import com.krachtix.commons.dto.Email
import com.krachtix.commons.dto.PhoneNumber

@Converter
class PhoneNumberConverter : AttributeConverter<PhoneNumber?, String?> {

    override fun convertToDatabaseColumn(phoneNumber: PhoneNumber?): String? {
        return phoneNumber?.e164
    }

    override fun convertToEntityAttribute(dbData: String?): PhoneNumber? {
        return dbData?.takeIf { it.isNotBlank() }?.let { PhoneNumber.fromE164(it) }
    }
}

@Converter
class EmailConverter : AttributeConverter<Email?, String?> {

    override fun convertToDatabaseColumn(email: Email?): String? {
        return email?.value
    }

    override fun convertToEntityAttribute(dbData: String?): Email? {
        return dbData?.takeIf { it.isNotBlank() }?.let { Email(it) }
    }
}
