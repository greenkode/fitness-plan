package com.krachtix.identity.unit.domain

import com.krachtix.commons.dto.PhoneNumber
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Locale

class PhoneNumberTest {

    @Nested
    inner class FromE164 {

        @Test
        fun `should parse valid E164 phone number`() {
            val phoneNumber = PhoneNumber.fromE164("+2348012345678")

            assertThat(phoneNumber.e164).isEqualTo("+2348012345678")
            assertThat(phoneNumber.regionCode).isEqualTo("NG")
        }

        @Test
        fun `should parse US E164 phone number`() {
            val phoneNumber = PhoneNumber.fromE164("+14155551234")

            assertThat(phoneNumber.e164).isEqualTo("+14155551234")
            assertThat(phoneNumber.regionCode).isEqualTo("US")
        }

        @Test
        fun `should parse UK E164 phone number`() {
            val phoneNumber = PhoneNumber.fromE164("+442071234567")

            assertThat(phoneNumber.e164).isEqualTo("+442071234567")
            assertThat(phoneNumber.regionCode).isEqualTo("GB")
        }

        @Test
        fun `should throw exception for invalid E164 format`() {
            assertThatThrownBy { PhoneNumber.fromE164("invalid") }
                .isInstanceOf(Exception::class.java)
        }
    }

    @Nested
    inner class FromE164OrNull {

        @Test
        fun `should return null for null input`() {
            val result = PhoneNumber.fromE164OrNull(null)

            assertThat(result).isNull()
        }

        @Test
        fun `should return null for blank input`() {
            val result = PhoneNumber.fromE164OrNull("  ")

            assertThat(result).isNull()
        }

        @Test
        fun `should return null for empty input`() {
            val result = PhoneNumber.fromE164OrNull("")

            assertThat(result).isNull()
        }

        @Test
        fun `should return PhoneNumber for valid E164`() {
            val result = PhoneNumber.fromE164OrNull("+2348012345678")

            assertThat(result).isNotNull
            assertThat(result?.e164).isEqualTo("+2348012345678")
        }

        @Test
        fun `should return null for invalid E164`() {
            val result = PhoneNumber.fromE164OrNull("invalid-phone")

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class FromRawNumber {

        @Test
        fun `should parse Nigerian local number`() {
            val locale = Locale.of("en", "NG")
            val phoneNumber = PhoneNumber.fromRawNumber("08012345678", locale)

            assertThat(phoneNumber.e164).isEqualTo("+2348012345678")
            assertThat(phoneNumber.regionCode).isEqualTo("NG")
        }

        @Test
        fun `should parse US local number`() {
            val locale = Locale.US
            val phoneNumber = PhoneNumber.fromRawNumber("(415) 555-1234", locale)

            assertThat(phoneNumber.e164).isEqualTo("+14155551234")
            assertThat(phoneNumber.regionCode).isEqualTo("US")
        }

        @Test
        fun `should parse UK local number`() {
            val locale = Locale.UK
            val phoneNumber = PhoneNumber.fromRawNumber("020 7123 4567", locale)

            assertThat(phoneNumber.e164).isEqualTo("+442071234567")
            assertThat(phoneNumber.regionCode).isEqualTo("GB")
        }

        @Test
        fun `should parse number with international prefix`() {
            val locale = Locale.of("en", "NG")
            val phoneNumber = PhoneNumber.fromRawNumber("+2348012345678", locale)

            assertThat(phoneNumber.e164).isEqualTo("+2348012345678")
        }

        @Test
        fun `should throw exception for invalid number`() {
            val locale = Locale.of("en", "NG")

            assertThatThrownBy { PhoneNumber.fromRawNumber("12345", locale) }
                .isInstanceOf(Exception::class.java)
        }
    }

    @Nested
    inner class FromRawNumberOrNull {

        @Test
        fun `should return null for null raw number`() {
            val result = PhoneNumber.fromRawNumberOrNull(null, Locale.US)

            assertThat(result).isNull()
        }

        @Test
        fun `should return null for blank raw number`() {
            val result = PhoneNumber.fromRawNumberOrNull("  ", Locale.US)

            assertThat(result).isNull()
        }

        @Test
        fun `should return null for null locale`() {
            val result = PhoneNumber.fromRawNumberOrNull("08012345678", null)

            assertThat(result).isNull()
        }

        @Test
        fun `should return PhoneNumber for valid input`() {
            val locale = Locale.of("en", "NG")
            val result = PhoneNumber.fromRawNumberOrNull("08012345678", locale)

            assertThat(result).isNotNull
            assertThat(result?.e164).isEqualTo("+2348012345678")
        }

        @Test
        fun `should return null for invalid number`() {
            val result = PhoneNumber.fromRawNumberOrNull("abc", Locale.US)

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class ToNationalFormat {

        @Test
        fun `should convert Nigerian number to national format`() {
            val phoneNumber = PhoneNumber.fromE164("+2348012345678")

            val nationalFormat = phoneNumber.toNationalFormat()

            assertThat(nationalFormat).isEqualTo("08012345678")
        }

        @Test
        fun `should convert US number to national format`() {
            val phoneNumber = PhoneNumber.fromE164("+14155551234")

            val nationalFormat = phoneNumber.toNationalFormat()

            assertThat(nationalFormat).contains("415")
            assertThat(nationalFormat).contains("555")
            assertThat(nationalFormat).contains("1234")
        }
    }

    @Nested
    inner class MaskPhoneNumber {

        @Test
        fun `should mask phone number keeping last 4 digits`() {
            val phoneNumber = PhoneNumber.fromE164("+2348012345678")

            val masked = phoneNumber.maskPhoneNumber()

            assertThat(masked).isEqualTo("**********5678")
        }

        @Test
        fun `should mask phone number keeping custom remaining digits`() {
            val phoneNumber = PhoneNumber.fromE164("+2348012345678")

            val masked = phoneNumber.maskPhoneNumber(6)

            assertThat(masked).isEqualTo("********345678")
        }

        @Test
        fun `should return full number when remaining is greater than length`() {
            val phoneNumber = PhoneNumber.fromE164("+2348012345678")

            val masked = phoneNumber.maskPhoneNumber(20)

            assertThat(masked).isEqualTo("+2348012345678")
        }
    }

    @Nested
    inner class RegionCode {

        @Test
        fun `should return NG for Nigerian numbers`() {
            val phoneNumber = PhoneNumber.fromE164("+2348012345678")

            assertThat(phoneNumber.regionCode).isEqualTo("NG")
        }

        @Test
        fun `should return US for American numbers`() {
            val phoneNumber = PhoneNumber.fromE164("+14155551234")

            assertThat(phoneNumber.regionCode).isEqualTo("US")
        }

        @Test
        fun `should return GB for UK numbers`() {
            val phoneNumber = PhoneNumber.fromE164("+442071234567")

            assertThat(phoneNumber.regionCode).isEqualTo("GB")
        }
    }
}
