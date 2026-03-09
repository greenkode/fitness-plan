package com.krachtix.identity.integration

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.kyc.TrustLevel
import com.krachtix.identity.config.BaseIntegrationTest
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.RegistrationSource
import com.krachtix.identity.core.entity.UserType
import com.krachtix.identity.core.profile.command.ChangePasswordCommand
import com.krachtix.identity.core.profile.command.ChangePasswordCommandHandler
import com.krachtix.identity.core.repository.OAuthUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
class ChangePasswordIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var changePasswordCommandHandler: ChangePasswordCommandHandler

    @Autowired
    private lateinit var userRepository: OAuthUserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var selfRegisteredUser: OAuthUser
    private lateinit var oauthGoogleUser: OAuthUser
    private val currentPassword = "CurrentPassword123!"
    private val newPassword = "NewPassword456!"

    @BeforeEach
    fun setUpTestData() {
        selfRegisteredUser = OAuthUser(
            username = "self-reg@example.com",
            password = passwordEncoder.encode(currentPassword)!!,
            email = Email("self-reg@example.com")
        ).apply {
            this.firstName = "Self"
            this.lastName = "Registered"
            this.userType = UserType.BUSINESS
            this.trustLevel = TrustLevel.TIER_ONE
            this.registrationSource = RegistrationSource.SELF_REGISTRATION
            this.enabled = true
        }
        userRepository.save(selfRegisteredUser)

        oauthGoogleUser = OAuthUser(
            username = "google-user@example.com",
            password = passwordEncoder.encode("placeholder")!!,
            email = Email("google-user@example.com")
        ).apply {
            this.firstName = "Google"
            this.lastName = "User"
            this.userType = UserType.BUSINESS
            this.trustLevel = TrustLevel.TIER_ONE
            this.registrationSource = RegistrationSource.OAUTH_GOOGLE
            this.enabled = true
        }
        userRepository.save(oauthGoogleUser)
    }

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    private fun setSecurityContext(userId: String) {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject(userId)
            .claim("authorities", listOf("ROLE_MERCHANT_USER"))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
    }

    @Nested
    inner class SuccessfulPasswordChange {

        @Test
        fun `should change password for self-registered user`() {
            setSecurityContext(selfRegisteredUser.id.toString())

            val command = ChangePasswordCommand(
                currentPassword = currentPassword.toCharArray(),
                newPassword = newPassword.toCharArray()
            )

            val result = changePasswordCommandHandler.handle(command)

            assertThat(result.success).isTrue()

            userRepository.flush()
            val reloadedUser = userRepository.findById(selfRegisteredUser.id!!).orElseThrow()
            assertThat(passwordEncoder.matches(newPassword, reloadedUser.password)).isTrue()
            assertThat(passwordEncoder.matches(currentPassword, reloadedUser.password)).isFalse()
        }

        @Test
        fun `should zero char arrays after successful password change`() {
            setSecurityContext(selfRegisteredUser.id.toString())

            val currentChars = currentPassword.toCharArray()
            val newChars = newPassword.toCharArray()
            val command = ChangePasswordCommand(
                currentPassword = currentChars,
                newPassword = newChars
            )

            changePasswordCommandHandler.handle(command)

            assertThat(currentChars).containsOnly('\u0000')
            assertThat(newChars).containsOnly('\u0000')
        }
    }

    @Nested
    inner class OAuthUserRestriction {

        @Test
        fun `should reject password change for Google OAuth user`() {
            setSecurityContext(oauthGoogleUser.id.toString())

            val command = ChangePasswordCommand(
                currentPassword = "anything".toCharArray(),
                newPassword = "newpass".toCharArray()
            )

            assertThatThrownBy { changePasswordCommandHandler.handle(command) }
                .isInstanceOf(InvalidRequestException::class.java)
        }

        @Test
        fun `should reject password change for Microsoft OAuth user`() {
            val microsoftUser = OAuthUser(
                username = "ms-user@example.com",
                password = passwordEncoder.encode("placeholder")!!,
                email = Email("ms-user@example.com")
            ).apply {
                this.firstName = "Microsoft"
                this.lastName = "User"
                this.userType = UserType.BUSINESS
                this.trustLevel = TrustLevel.TIER_ONE
                this.registrationSource = RegistrationSource.OAUTH_MICROSOFT
                this.enabled = true
            }
            userRepository.save(microsoftUser)

            setSecurityContext(microsoftUser.id.toString())

            val command = ChangePasswordCommand(
                currentPassword = "anything".toCharArray(),
                newPassword = "newpass".toCharArray()
            )

            assertThatThrownBy { changePasswordCommandHandler.handle(command) }
                .isInstanceOf(InvalidRequestException::class.java)
        }
    }

    @Nested
    inner class PasswordValidation {

        @Test
        fun `should reject when current password is incorrect`() {
            setSecurityContext(selfRegisteredUser.id.toString())

            val command = ChangePasswordCommand(
                currentPassword = "WrongPassword123!".toCharArray(),
                newPassword = newPassword.toCharArray()
            )

            assertThatThrownBy { changePasswordCommandHandler.handle(command) }
                .isInstanceOf(InvalidRequestException::class.java)

            userRepository.flush()
            val reloadedUser = userRepository.findById(selfRegisteredUser.id!!).orElseThrow()
            assertThat(passwordEncoder.matches(currentPassword, reloadedUser.password)).isTrue()
        }

        @Test
        fun `should reject when new password is same as current`() {
            setSecurityContext(selfRegisteredUser.id.toString())

            val command = ChangePasswordCommand(
                currentPassword = currentPassword.toCharArray(),
                newPassword = currentPassword.toCharArray()
            )

            assertThatThrownBy { changePasswordCommandHandler.handle(command) }
                .isInstanceOf(InvalidRequestException::class.java)
        }

        @Test
        fun `should zero char arrays even when password change fails`() {
            setSecurityContext(selfRegisteredUser.id.toString())

            val currentChars = "WrongPassword123!".toCharArray()
            val newChars = newPassword.toCharArray()
            val command = ChangePasswordCommand(
                currentPassword = currentChars,
                newPassword = newChars
            )

            runCatching { changePasswordCommandHandler.handle(command) }

            assertThat(currentChars).containsOnly('\u0000')
            assertThat(newChars).containsOnly('\u0000')
        }
    }
}
