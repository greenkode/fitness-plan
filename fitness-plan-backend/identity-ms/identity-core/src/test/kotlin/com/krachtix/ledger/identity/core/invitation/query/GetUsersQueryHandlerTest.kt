package com.krachtix.identity.core.invitation.query

import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.invitation.dto.UserStatus
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetUsersQueryHandlerTest {

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var userService: UserService

    @InjectMocks
    private lateinit var handler: GetUsersQueryHandler

    private val merchantId = UUID.randomUUID()
    private lateinit var currentUser: OAuthUser

    @BeforeEach
    fun setUp() {
        currentUser = OAuthUser(username = "current@company.com", password = "encoded").apply {
            id = UUID.randomUUID()
            merchantId = this@GetUsersQueryHandlerTest.merchantId
        }
    }

    private fun createUser(
        username: String,
        enabled: Boolean = true,
        active: Boolean = true,
        firstName: String? = null,
        lastName: String? = null,
        authorities: MutableSet<String> = mutableSetOf()
    ): OAuthUser = OAuthUser(username = username, password = "encoded").apply {
        id = UUID.randomUUID()
        merchantId = this@GetUsersQueryHandlerTest.merchantId
        this.enabled = enabled
        this.firstName = firstName
        this.lastName = lastName
        this.authorities = authorities
        invitationStatus = active
    }

    @Nested
    inner class UserRetrieval {

        @Test
        fun `should return active users with ACTIVE status`() {
            val user = createUser(username = "active@company.com", active = true)

            whenever(userService.getCurrentUser()).thenReturn(currentUser)
            whenever(userRepository.findByMerchantId(merchantId)).thenReturn(listOf(user))

            val result = handler.handle(GetUsersQuery())

            assertThat(result.users).hasSize(1)
            assertThat(result.users[0].status).isEqualTo(UserStatus.ACTIVE)
            assertThat(result.users[0].username).isEqualTo("active@company.com")
        }

        @Test
        fun `should return pending users with PENDING status`() {
            val user = createUser(username = "pending@company.com", active = false)

            whenever(userService.getCurrentUser()).thenReturn(currentUser)
            whenever(userRepository.findByMerchantId(merchantId)).thenReturn(listOf(user))

            val result = handler.handle(GetUsersQuery())

            assertThat(result.users).hasSize(1)
            assertThat(result.users[0].status).isEqualTo(UserStatus.PENDING)
        }

        @Test
        fun `should map roles correctly`() {
            val user = createUser(
                username = "admin@company.com",
                authorities = mutableSetOf("ROLE_MERCHANT_ADMIN")
            )

            whenever(userService.getCurrentUser()).thenReturn(currentUser)
            whenever(userRepository.findByMerchantId(merchantId)).thenReturn(listOf(user))

            val result = handler.handle(GetUsersQuery())

            assertThat(result.users).hasSize(1)
            assertThat(result.users[0].role).isEqualTo("Admin Role")
        }

        @Test
        fun `should show no roles assigned for user without recognized roles`() {
            val user = createUser(
                username = "norole@company.com",
                authorities = mutableSetOf("SOME_OTHER_AUTHORITY")
            )

            whenever(userService.getCurrentUser()).thenReturn(currentUser)
            whenever(userRepository.findByMerchantId(merchantId)).thenReturn(listOf(user))

            val result = handler.handle(GetUsersQuery())

            assertThat(result.users).hasSize(1)
            assertThat(result.users[0].role).isEqualTo("No roles assigned")
        }

        @Test
        fun `should use fullName from firstName and lastName`() {
            val user = createUser(username = "john@company.com", firstName = "John", lastName = "Doe")

            whenever(userService.getCurrentUser()).thenReturn(currentUser)
            whenever(userRepository.findByMerchantId(merchantId)).thenReturn(listOf(user))

            val result = handler.handle(GetUsersQuery())

            assertThat(result.users).hasSize(1)
            assertThat(result.users[0].fullName).isEqualTo("John Doe")
        }

        @Test
        fun `should use username as fullName when names are null`() {
            val user = createUser(username = "fallback@company.com")

            whenever(userService.getCurrentUser()).thenReturn(currentUser)
            whenever(userRepository.findByMerchantId(merchantId)).thenReturn(listOf(user))

            val result = handler.handle(GetUsersQuery())

            assertThat(result.users).hasSize(1)
            assertThat(result.users[0].fullName).isEqualTo("fallback@company.com")
        }

        @Test
        fun `should query repository with current user merchant id`() {
            whenever(userService.getCurrentUser()).thenReturn(currentUser)
            whenever(userRepository.findByMerchantId(merchantId)).thenReturn(emptyList())

            handler.handle(GetUsersQuery())

            verify(userRepository).findByMerchantId(merchantId)
        }
    }

    @Nested
    inner class UserFiltering {

        @Test
        fun `should filter out disabled users`() {
            val enabledUser = createUser(username = "enabled@company.com", enabled = true)
            val disabledUser = createUser(username = "disabled@company.com", enabled = false)

            whenever(userService.getCurrentUser()).thenReturn(currentUser)
            whenever(userRepository.findByMerchantId(merchantId)).thenReturn(listOf(enabledUser, disabledUser))

            val result = handler.handle(GetUsersQuery())

            assertThat(result.users).hasSize(1)
            assertThat(result.users[0].username).isEqualTo("enabled@company.com")
        }

        @Test
        fun `should return empty list when no enabled users found`() {
            whenever(userService.getCurrentUser()).thenReturn(currentUser)
            whenever(userRepository.findByMerchantId(merchantId)).thenReturn(emptyList())

            val result = handler.handle(GetUsersQuery())

            assertThat(result.users).isEmpty()
        }

        @Test
        fun `should return empty list when all users are disabled`() {
            val disabled1 = createUser(username = "d1@company.com", enabled = false)
            val disabled2 = createUser(username = "d2@company.com", enabled = false)

            whenever(userService.getCurrentUser()).thenReturn(currentUser)
            whenever(userRepository.findByMerchantId(merchantId)).thenReturn(listOf(disabled1, disabled2))

            val result = handler.handle(GetUsersQuery())

            assertThat(result.users).isEmpty()
        }
    }

    @Nested
    inner class ErrorHandling {

        @Test
        fun `should throw RecordNotFoundException when user has no merchantId`() {
            val userWithoutMerchant = OAuthUser(username = "orphan@company.com", password = "encoded").apply {
                id = UUID.randomUUID()
                merchantId = null
            }

            whenever(userService.getCurrentUser()).thenReturn(userWithoutMerchant)

            assertThatThrownBy { handler.handle(GetUsersQuery()) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("User is not associated with a merchant")
        }
    }
}
