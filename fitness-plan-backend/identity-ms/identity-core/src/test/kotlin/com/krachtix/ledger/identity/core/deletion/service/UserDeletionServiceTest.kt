package com.krachtix.identity.core.deletion.service

import com.krachtix.commons.dto.Email
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.refreshtoken.domain.RefreshTokenRepository
import com.krachtix.identity.core.repository.OAuthProviderAccountRepository
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.trusteddevice.domain.TrustedDeviceRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserDeletionServiceTest {

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var providerAccountRepository: OAuthProviderAccountRepository

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    private lateinit var trustedDeviceRepository: TrustedDeviceRepository

    @InjectMocks
    private lateinit var userDeletionService: UserDeletionService

    private val userId = UUID.randomUUID()
    private lateinit var user: OAuthUser

    @BeforeEach
    fun setUp() {
        user = OAuthUser(
            username = "john.doe",
            password = "encodedPassword",
            email = Email("john@example.com")
        ).apply {
            id = userId
            firstName = "John"
            middleName = "Michael"
            lastName = "Doe"
            pictureUrl = "https://example.com/photo.jpg"
            dateOfBirth = LocalDate.of(1990, 1, 15)
            taxIdentificationNumber = "TAX123456"
            merchantId = UUID.randomUUID()
            organizationId = UUID.randomUUID()
            enabled = true
            accountNonExpired = true
            accountNonLocked = true
            credentialsNonExpired = true
            authorities.add("ROLE_USER")
            authorities.add("ROLE_ADMIN")
        }
    }

    @Nested
    inner class SuccessfulDeletion {

        @BeforeEach
        fun setUp() {
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(refreshTokenRepository.revokeAllUserTokens(any(), any())).thenReturn(3)
            whenever(providerAccountRepository.anonymizeByUserId(any(), any(), any())).thenReturn(1)
            whenever(userRepository.save(any<OAuthUser>())).thenAnswer { it.arguments[0] }
        }

        @Test
        fun `should return success result`() {
            val result = userDeletionService.deleteUser(userId)

            assertThat(result.success).isTrue()
            assertThat(result.message).isEqualTo("user.deleted_successfully")
            assertThat(result.userId).isEqualTo(userId)
        }

        @Test
        fun `should set email to null`() {
            userDeletionService.deleteUser(userId)

            assertThat(user.email).isNull()
        }

        @Test
        fun `should set merchantId to null`() {
            userDeletionService.deleteUser(userId)

            assertThat(user.merchantId).isNull()
        }

        @Test
        fun `should set organizationId to null`() {
            userDeletionService.deleteUser(userId)

            assertThat(user.organizationId).isNull()
        }

        @Test
        fun `should disable user account flags`() {
            userDeletionService.deleteUser(userId)

            assertThat(user.enabled).isFalse()
            assertThat(user.accountNonExpired).isFalse()
            assertThat(user.accountNonLocked).isFalse()
            assertThat(user.credentialsNonExpired).isFalse()
        }

        @Test
        fun `should clear all PII fields`() {
            userDeletionService.deleteUser(userId)

            assertThat(user.firstName).isNull()
            assertThat(user.lastName).isNull()
            assertThat(user.middleName).isNull()
            assertThat(user.phoneNumber).isNull()
            assertThat(user.pictureUrl).isNull()
            assertThat(user.dateOfBirth).isNull()
            assertThat(user.taxIdentificationNumber).isNull()
        }

        @Test
        fun `should set username to anonymized format`() {
            userDeletionService.deleteUser(userId)

            assertThat(user.username).isEqualTo("deleted_user_${userId.toString().take(8)}")
        }

        @Test
        fun `should clear password`() {
            userDeletionService.deleteUser(userId)

            assertThat(user.password).isEmpty()
        }

        @Test
        fun `should revoke all refresh tokens`() {
            userDeletionService.deleteUser(userId)

            verify(refreshTokenRepository).revokeAllUserTokens(any(), any())
        }

        @Test
        fun `should delete all trusted devices`() {
            userDeletionService.deleteUser(userId)

            verify(trustedDeviceRepository).deleteAllByUserId(userId)
        }

        @Test
        fun `should anonymize provider accounts`() {
            userDeletionService.deleteUser(userId)

            verify(providerAccountRepository).anonymizeByUserId(any(), any(), any())
        }
    }

    @Nested
    inner class UserNotFound {

        @BeforeEach
        fun setUp() {
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())
        }

        @Test
        fun `should return failure result when user not found`() {
            val result = userDeletionService.deleteUser(userId)

            assertThat(result.success).isFalse()
            assertThat(result.message).isEqualTo("error.user_not_found")
            assertThat(result.userId).isEqualTo(userId)
        }

        @Test
        fun `should not revoke tokens when user not found`() {
            userDeletionService.deleteUser(userId)

            verify(refreshTokenRepository, never()).revokeAllUserTokens(any(), any())
        }

        @Test
        fun `should not delete trusted devices when user not found`() {
            userDeletionService.deleteUser(userId)

            verify(trustedDeviceRepository, never()).deleteAllByUserId(any())
        }

        @Test
        fun `should not anonymize provider accounts when user not found`() {
            userDeletionService.deleteUser(userId)

            verify(providerAccountRepository, never()).anonymizeByUserId(any(), any(), any())
        }

        @Test
        fun `should not save user when user not found`() {
            userDeletionService.deleteUser(userId)

            verify(userRepository, never()).save(any<OAuthUser>())
        }
    }
}
