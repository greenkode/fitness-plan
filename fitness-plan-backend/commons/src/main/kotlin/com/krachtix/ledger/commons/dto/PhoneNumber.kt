package com.krachtix.commons.dto

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.InvalidRequestException
import java.io.Serializable
import java.util.Locale

data class PhoneNumber private constructor(
    val e164: String,
    private val parsedNumber: Phonenumber.PhoneNumber
) : Serializable {

    val regionCode: String
        get() = PhoneNumberUtil.getInstance().getRegionCodeForNumber(parsedNumber)

    fun toNationalFormat(): String {
        val util = PhoneNumberUtil.getInstance()
        val numberInNationalFormat = util.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
            .replace(" ", "")
        return numberInNationalFormat.replace("-", "")
    }

    fun maskPhoneNumber(remaining: Int = 4): String {
        if (e164.length <= remaining) {
            return e164
        }
        val maskLen = e164.length - remaining
        return "*".repeat(maskLen) + e164.substring(maskLen)
    }

    companion object {
        private val log = KotlinLogging.logger {}

        fun fromE164(e164Value: String): PhoneNumber {
            val util = PhoneNumberUtil.getInstance()
            val parsed = util.parse(e164Value, null)

            if (!util.isValidNumber(parsed)) {
                throw InvalidRequestException("Invalid E164 phone number: $e164Value")
            }

            return PhoneNumber(e164Value, parsed)
        }

        fun fromE164OrNull(e164Value: String?): PhoneNumber? {
            if (e164Value.isNullOrBlank()) return null
            return runCatching { fromE164(e164Value) }
                .onFailure { log.error(it) { "Failed to parse E164 phone number: $e164Value" } }
                .getOrNull()
        }

        fun fromRawNumber(rawNumber: String, locale: Locale): PhoneNumber {
            val util = PhoneNumberUtil.getInstance()

            try {
                val parsed = util.parse(rawNumber, locale.country)

                if (!util.isValidNumber(parsed)) {
                    throw InvalidRequestException(
                        "The phone number $rawNumber is not a valid ${locale.country} number."
                    )
                }

                val e164 = util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                return PhoneNumber(e164, parsed)
            } catch (e: NumberParseException) {
                log.error(e) { "Invalid phone number format [$rawNumber]" }
                throw InvalidRequestException("Invalid phone number format [$rawNumber]")
            }
        }

        fun fromRawNumberOrNull(rawNumber: String?, locale: Locale?): PhoneNumber? {
            if (rawNumber.isNullOrBlank() || locale == null) return null
            return runCatching { fromRawNumber(rawNumber, locale) }
                .onFailure { log.error(it) { "Failed to parse phone number: $rawNumber" } }
                .getOrNull()
        }
    }
}
