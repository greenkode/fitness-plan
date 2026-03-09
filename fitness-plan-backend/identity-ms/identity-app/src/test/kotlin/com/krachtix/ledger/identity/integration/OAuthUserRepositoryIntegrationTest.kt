package com.krachtix.identity.integration

import com.krachtix.commons.dto.Email
import com.krachtix.commons.dto.PhoneNumber
import com.krachtix.commons.kyc.TrustLevel
import com.krachtix.identity.config.BaseIntegrationTest
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.RegistrationSource
import com.krachtix.identity.core.entity.UserType
import com.krachtix.identity.core.repository.OAuthUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Locale
import java.util.UUID

class OAuthUserRepositoryIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var userRepository: OAuthUserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Nested
    inner class PhoneNumberPersistence {

        @Test
        fun `should persist phone number in E164 format`() {
            val phoneNumber = PhoneNumber.fromRawNumber("08012345678", Locale.of("en", "NG"))
            val user = createUser(
                email = "phone-test-1@example.com",
                phoneNumber = phoneNumber
            )

            val savedUser = userRepository.save(user)
            userRepository.flush()

            val foundUser = userRepository.findById(savedUser.id!!).orElseThrow()
            assertThat(foundUser.phoneNumber).isNotNull
            assertThat(foundUser.phoneNumber?.e164).isEqualTo("+2348012345678")
            assertThat(foundUser.phoneNumber?.regionCode).isEqualTo("NG")
        }

        @Test
        fun `should persist US phone number in E164 format`() {
            val phoneNumber = PhoneNumber.fromRawNumber("(415) 555-1234", Locale.US)
            val user = createUser(
                email = "phone-test-2@example.com",
                phoneNumber = phoneNumber
            )

            val savedUser = userRepository.save(user)
            userRepository.flush()

            val foundUser = userRepository.findById(savedUser.id!!).orElseThrow()
            assertThat(foundUser.phoneNumber?.e164).isEqualTo("+14155551234")
            assertThat(foundUser.phoneNumber?.regionCode).isEqualTo("US")
        }

        @Test
        fun `should persist null phone number`() {
            val user = createUser(
                email = "phone-test-3@example.com",
                phoneNumber = null
            )

            val savedUser = userRepository.save(user)
            userRepository.flush()

            val foundUser = userRepository.findById(savedUser.id!!).orElseThrow()
            assertThat(foundUser.phoneNumber).isNull()
        }

        @Test
        fun `should load phone number from E164 stored value`() {
            val phoneNumber = PhoneNumber.fromE164("+2348012345678")
            val user = createUser(
                email = "phone-test-4@example.com",
                phoneNumber = phoneNumber
            )

            val savedUser = userRepository.save(user)
            userRepository.flush()

            val foundUser = userRepository.findById(savedUser.id!!).orElseThrow()
            assertThat(foundUser.phoneNumber?.toNationalFormat()).isEqualTo("08012345678")
        }
    }

    @Nested
    inner class EmailPersistence {

        @Test
        fun `should persist email domain object`() {
            val email = Email("test-email-1@example.com")
            val user = createUser(email = email.value)

            val savedUser = userRepository.save(user)
            userRepository.flush()

            val foundUser = userRepository.findById(savedUser.id!!).orElseThrow()
            assertThat(foundUser.email).isNotNull
            assertThat(foundUser.email?.value).isEqualTo("test-email-1@example.com")
        }

        @Test
        fun `should find user by email string`() {
            val email = "find-by-email@example.com"
            val user = createUser(email = email)
            userRepository.save(user)
            userRepository.flush()

            val foundUser = userRepository.findByEmail(Email(email))

            assertThat(foundUser).isNotNull
            assertThat(foundUser?.email?.value).isEqualTo(email)
        }

        @Test
        fun `should persist email with special characters`() {
            val email = Email("test+special_chars@example.com")
            val user = createUser(email = email.value)

            val savedUser = userRepository.save(user)
            userRepository.flush()

            val foundUser = userRepository.findById(savedUser.id!!).orElseThrow()
            assertThat(foundUser.email?.value).isEqualTo("test+special_chars@example.com")
        }
    }

    @Nested
    inner class CombinedPersistence {

        @Test
        fun `should persist user with both email and phone number`() {
            val email = Email("combined-test@example.com")
            val phoneNumber = PhoneNumber.fromRawNumber("08012345678", Locale.of("en", "NG"))

            val user = createUser(
                email = email.value,
                phoneNumber = phoneNumber
            )

            val savedUser = userRepository.save(user)
            userRepository.flush()

            val foundUser = userRepository.findById(savedUser.id!!).orElseThrow()
            assertThat(foundUser.email?.value).isEqualTo("combined-test@example.com")
            assertThat(foundUser.phoneNumber?.e164).isEqualTo("+2348012345678")
        }

        @Test
        fun `should update phone number on existing user`() {
            val user = createUser(
                email = "update-phone@example.com",
                phoneNumber = PhoneNumber.fromE164("+2348012345678")
            )
            val savedUser = userRepository.save(user)
            userRepository.flush()

            savedUser.phoneNumber = PhoneNumber.fromE164("+14155551234")
            userRepository.save(savedUser)
            userRepository.flush()

            val foundUser = userRepository.findById(savedUser.id!!).orElseThrow()
            assertThat(foundUser.phoneNumber?.e164).isEqualTo("+14155551234")
            assertThat(foundUser.phoneNumber?.regionCode).isEqualTo("US")
        }

        @Test
        fun `should set phone number to null on existing user`() {
            val user = createUser(
                email = "clear-phone@example.com",
                phoneNumber = PhoneNumber.fromE164("+2348012345678")
            )
            val savedUser = userRepository.save(user)
            userRepository.flush()

            savedUser.phoneNumber = null
            userRepository.save(savedUser)
            userRepository.flush()

            val foundUser = userRepository.findById(savedUser.id!!).orElseThrow()
            assertThat(foundUser.phoneNumber).isNull()
        }
    }

    private fun createUser(
        email: String,
        phoneNumber: PhoneNumber? = null
    ): OAuthUser {
        return OAuthUser(
            username = email,
            password = passwordEncoder.encode("TestPassword123!")!!,
            email = Email(email)
        ).apply {
            this.phoneNumber = phoneNumber
            this.firstName = "Test"
            this.lastName = "User"
            this.userType = UserType.BUSINESS
            this.trustLevel = TrustLevel.TIER_ONE
            this.registrationSource = RegistrationSource.SELF_REGISTRATION
            this.enabled = true
            this.merchantId = null
            this.organizationId = null
        }
    }
}
