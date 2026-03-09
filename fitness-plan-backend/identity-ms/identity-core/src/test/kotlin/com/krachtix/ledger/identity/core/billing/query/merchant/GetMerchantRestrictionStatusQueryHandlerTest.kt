package com.krachtix.identity.core.billing.query.merchant

import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.organization.entity.Organization
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetMerchantRestrictionStatusQueryHandlerTest {

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var userService: UserService

    private lateinit var handler: GetMerchantRestrictionStatusQueryHandler

    private val merchantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = GetMerchantRestrictionStatusQueryHandler(organizationRepository, userService)
    }

    private fun mockCurrentUser(organizationId: UUID?) {
        val user = Mockito.mock(OAuthUser::class.java)
        whenever(user.organizationId).thenReturn(organizationId)
        whenever(userService.getCurrentUser()).thenReturn(user)
    }

    @Nested
    @DisplayName("Restricted Organization")
    inner class RestrictedOrganization {

        @Test
        fun `should return restricted true when organization is restricted`() {
            mockCurrentUser(merchantId)

            val organization = Organization(
                id = merchantId,
                name = "Test Org",
                slug = "test-org",
                restricted = true,
                restrictedAt = Instant.now(),
                restrictionReason = "Overdue invoice"
            )
            whenever(organizationRepository.findById(merchantId)).thenReturn(Optional.of(organization))

            val result = handler.handle(GetMerchantRestrictionStatusQuery())

            assertThat(result.response.restricted).isTrue()
            assertThat(result.response.reason).isEqualTo("Overdue invoice")
        }
    }

    @Nested
    @DisplayName("Unrestricted Organization")
    inner class UnrestrictedOrganization {

        @Test
        fun `should return restricted false when organization is not restricted`() {
            mockCurrentUser(merchantId)

            val organization = Organization(
                id = merchantId,
                name = "Test Org",
                slug = "test-org",
                restricted = false
            )
            whenever(organizationRepository.findById(merchantId)).thenReturn(Optional.of(organization))

            val result = handler.handle(GetMerchantRestrictionStatusQuery())

            assertThat(result.response.restricted).isFalse()
            assertThat(result.response.reason).isNull()
        }
    }

    @Nested
    @DisplayName("Organization Not Found")
    inner class OrganizationNotFound {

        @Test
        fun `should default to restricted false when organization not found`() {
            mockCurrentUser(merchantId)

            whenever(organizationRepository.findById(merchantId)).thenReturn(Optional.empty())

            val result = handler.handle(GetMerchantRestrictionStatusQuery())

            assertThat(result.response.restricted).isFalse()
            assertThat(result.response.reason).isNull()
        }
    }

    @Nested
    @DisplayName("No Organization")
    inner class NoOrganization {

        @Test
        fun `should throw IllegalStateException when user has no organization`() {
            mockCurrentUser(null)

            assertThatThrownBy { handler.handle(GetMerchantRestrictionStatusQuery()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User has no organization")
        }
    }
}
