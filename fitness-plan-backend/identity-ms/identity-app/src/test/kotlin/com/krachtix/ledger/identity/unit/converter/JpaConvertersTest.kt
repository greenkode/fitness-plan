package com.krachtix.identity.unit.converter

import com.krachtix.commons.dto.Email
import com.krachtix.commons.dto.PhoneNumber
import com.krachtix.identity.core.entity.EmailConverter
import com.krachtix.identity.core.entity.PhoneNumberConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Locale

class JpaConvertersTest {

    @Nested
    inner class PhoneNumberConverterTest {

        private val converter = PhoneNumberConverter()

        @Nested
        inner class ConvertToDatabaseColumn {

            @Test
            fun `should return null for null phone number`() {
                val result = converter.convertToDatabaseColumn(null)

                assertThat(result).isNull()
            }

            @Test
            fun `should return E164 format for valid phone number`() {
                val phoneNumber = PhoneNumber.fromE164("+2348012345678")

                val result = converter.convertToDatabaseColumn(phoneNumber)

                assertThat(result).isEqualTo("+2348012345678")
            }

            @Test
            fun `should store phone number from raw input as E164`() {
                val locale = Locale.of("en", "NG")
                val phoneNumber = PhoneNumber.fromRawNumber("08012345678", locale)

                val result = converter.convertToDatabaseColumn(phoneNumber)

                assertThat(result).isEqualTo("+2348012345678")
            }
        }

        @Nested
        inner class ConvertToEntityAttribute {

            @Test
            fun `should return null for null database value`() {
                val result = converter.convertToEntityAttribute(null)

                assertThat(result).isNull()
            }

            @Test
            fun `should return null for empty string`() {
                val result = converter.convertToEntityAttribute("")

                assertThat(result).isNull()
            }

            @Test
            fun `should return null for blank string`() {
                val result = converter.convertToEntityAttribute("   ")

                assertThat(result).isNull()
            }

            @Test
            fun `should return PhoneNumber for valid E164`() {
                val result = converter.convertToEntityAttribute("+2348012345678")

                assertThat(result).isNotNull
                assertThat(result?.e164).isEqualTo("+2348012345678")
                assertThat(result?.regionCode).isEqualTo("NG")
            }

            @Test
            fun `should return PhoneNumber for US number`() {
                val result = converter.convertToEntityAttribute("+14155551234")

                assertThat(result).isNotNull
                assertThat(result?.e164).isEqualTo("+14155551234")
                assertThat(result?.regionCode).isEqualTo("US")
            }
        }

        @Nested
        inner class RoundTrip {

            @Test
            fun `should round trip Nigerian phone number`() {
                val original = PhoneNumber.fromRawNumber("08012345678", Locale.of("en", "NG"))

                val dbValue = converter.convertToDatabaseColumn(original)
                val restored = converter.convertToEntityAttribute(dbValue)

                assertThat(restored?.e164).isEqualTo(original.e164)
                assertThat(restored?.regionCode).isEqualTo(original.regionCode)
            }

            @Test
            fun `should round trip US phone number`() {
                val original = PhoneNumber.fromRawNumber("(415) 555-1234", Locale.US)

                val dbValue = converter.convertToDatabaseColumn(original)
                val restored = converter.convertToEntityAttribute(dbValue)

                assertThat(restored?.e164).isEqualTo(original.e164)
                assertThat(restored?.regionCode).isEqualTo("US")
            }

            @Test
            fun `should handle null round trip`() {
                val dbValue = converter.convertToDatabaseColumn(null)
                val restored = converter.convertToEntityAttribute(dbValue)

                assertThat(restored).isNull()
            }
        }
    }

    @Nested
    inner class EmailConverterTest {

        private val converter = EmailConverter()

        @Nested
        inner class ConvertToDatabaseColumn {

            @Test
            fun `should return null for null email`() {
                val result = converter.convertToDatabaseColumn(null)

                assertThat(result).isNull()
            }

            @Test
            fun `should return email value`() {
                val email = Email("test@example.com")

                val result = converter.convertToDatabaseColumn(email)

                assertThat(result).isEqualTo("test@example.com")
            }

            @Test
            fun `should preserve case of email`() {
                val email = Email("Test@Example.COM")

                val result = converter.convertToDatabaseColumn(email)

                assertThat(result).isEqualTo("Test@Example.COM")
            }
        }

        @Nested
        inner class ConvertToEntityAttribute {

            @Test
            fun `should return null for null database value`() {
                val result = converter.convertToEntityAttribute(null)

                assertThat(result).isNull()
            }

            @Test
            fun `should return null for empty string`() {
                val result = converter.convertToEntityAttribute("")

                assertThat(result).isNull()
            }

            @Test
            fun `should return null for blank string`() {
                val result = converter.convertToEntityAttribute("   ")

                assertThat(result).isNull()
            }

            @Test
            fun `should return Email for valid email string`() {
                val result = converter.convertToEntityAttribute("test@example.com")

                assertThat(result).isNotNull
                assertThat(result?.value).isEqualTo("test@example.com")
            }
        }

        @Nested
        inner class RoundTrip {

            @Test
            fun `should round trip email`() {
                val original = Email("test@example.com")

                val dbValue = converter.convertToDatabaseColumn(original)
                val restored = converter.convertToEntityAttribute(dbValue)

                assertThat(restored?.value).isEqualTo(original.value)
            }

            @Test
            fun `should handle null round trip`() {
                val dbValue = converter.convertToDatabaseColumn(null)
                val restored = converter.convertToEntityAttribute(dbValue)

                assertThat(restored).isNull()
            }
        }
    }
}
