package com.krachtix.identity.unit.domain

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.InvalidRequestException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EmailTest {

    @Nested
    inner class ValidEmails {

        @ParameterizedTest
        @ValueSource(strings = [
            "test@example.com",
            "user.name@domain.com",
            "user+tag@domain.com",
            "user_name@domain.org",
            "user123@domain.co.uk",
            "test@sub.domain.example.com"
        ])
        fun `should create Email for valid addresses`(emailAddress: String) {
            val email = Email(emailAddress)

            assertThat(email.value).isEqualTo(emailAddress)
        }

        @Test
        fun `should accept email with plus addressing`() {
            val email = Email("test+tag@example.com")

            assertThat(email.value).isEqualTo("test+tag@example.com")
        }

        @Test
        fun `should accept email with underscores`() {
            val email = Email("test_user@example.com")

            assertThat(email.value).isEqualTo("test_user@example.com")
        }

        @Test
        fun `should accept email with dots in local part`() {
            val email = Email("test.user@example.com")

            assertThat(email.value).isEqualTo("test.user@example.com")
        }

        @Test
        fun `should accept email with numbers`() {
            val email = Email("user123@example456.com")

            assertThat(email.value).isEqualTo("user123@example456.com")
        }
    }

    @Nested
    inner class InvalidEmails {

        @ParameterizedTest
        @ValueSource(strings = [
            "invalid",
            "invalid@",
            "@example.com",
            "invalid@.com",
            "invalid@com",
            "",
            " ",
            "invalid email@example.com"
        ])
        fun `should throw exception for invalid email addresses`(emailAddress: String) {
            assertThatThrownBy { Email(emailAddress) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessageContaining("not valid")
        }

        @Test
        fun `should throw exception for email without domain`() {
            assertThatThrownBy { Email("test@") }
                .isInstanceOf(InvalidRequestException::class.java)
        }

        @Test
        fun `should throw exception for email without local part`() {
            assertThatThrownBy { Email("@example.com") }
                .isInstanceOf(InvalidRequestException::class.java)
        }
    }

    @Nested
    inner class EmailEquality {

        @Test
        fun `should be equal for same email value`() {
            val email1 = Email("test@example.com")
            val email2 = Email("test@example.com")

            assertThat(email1).isEqualTo(email2)
            assertThat(email1.hashCode()).isEqualTo(email2.hashCode())
        }

        @Test
        fun `should not be equal for different email values`() {
            val email1 = Email("test1@example.com")
            val email2 = Email("test2@example.com")

            assertThat(email1).isNotEqualTo(email2)
        }

        @Test
        fun `should be case sensitive`() {
            val email1 = Email("Test@example.com")
            val email2 = Email("test@example.com")

            assertThat(email1).isNotEqualTo(email2)
        }
    }
}
