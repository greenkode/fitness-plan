package com.krachtix.identity.core.billing.query.merchant

import com.krachtix.identity.core.billing.entity.BillingCycle
import com.krachtix.identity.core.billing.entity.BillingPlanEntity
import com.krachtix.identity.core.billing.entity.BillingPlanStatus
import com.krachtix.identity.core.billing.service.BillingPlanService
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.organization.entity.Organization
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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetMerchantBillingPlanQueryHandlerTest {

    @Mock
    private lateinit var billingPlanService: BillingPlanService

    @Mock
    private lateinit var userService: UserService

    private lateinit var handler: GetMerchantBillingPlanQueryHandler

    private val merchantId = UUID.randomUUID()
    private val planPublicId = UUID.randomUUID()
    private val effectiveFrom = Instant.parse("2026-01-01T00:00:00Z")
    private val effectiveUntil = Instant.parse("2026-12-31T23:59:59Z")

    @BeforeEach
    fun setUp() {
        handler = GetMerchantBillingPlanQueryHandler(billingPlanService, userService)
    }

    private fun buildOAuthUserMock(organizationId: UUID?): OAuthUser {
        val user = Mockito.mock(OAuthUser::class.java)
        whenever(user.organizationId).thenReturn(organizationId)
        return user
    }

    private fun buildOrganizationMock(): Organization {
        val organization = Mockito.mock(Organization::class.java)
        whenever(organization.id).thenReturn(merchantId)
        return organization
    }

    private fun buildPlanEntity(organization: Organization): BillingPlanEntity =
        BillingPlanEntity(
            publicId = planPublicId,
            organization = organization,
            name = "Standard Plan",
            platformFeeAmount = BigDecimal("10.000"),
            perAccountFeeAmount = BigDecimal("0.500"),
            perTransactionFeeAmount = BigDecimal("0.100"),
            maxChargeAmount = BigDecimal("500.000"),
            currency = "USD",
            billingCycle = BillingCycle.MONTHLY,
            status = BillingPlanStatus.ACTIVE,
            effectiveFrom = effectiveFrom,
            effectiveUntil = effectiveUntil
        )

    @Nested
    @DisplayName("Active plan found")
    inner class ActivePlanFound {

        @Test
        fun `should return MerchantBillingPlanResponse with all fields mapped`() {
            val user = buildOAuthUserMock(merchantId)
            val organization = buildOrganizationMock()
            val entity = buildPlanEntity(organization)
            val query = GetMerchantBillingPlanQuery()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(billingPlanService.getActivePlan(merchantId)).thenReturn(entity)

            val result = handler.handle(query)

            assertThat(result.plan).isNotNull
            assertThat(result.plan!!.publicId).isEqualTo(planPublicId)
            assertThat(result.plan!!.name).isEqualTo("Standard Plan")
            assertThat(result.plan!!.platformFeeAmount).isEqualByComparingTo(BigDecimal("10.000"))
            assertThat(result.plan!!.perAccountFeeAmount).isEqualByComparingTo(BigDecimal("0.500"))
            assertThat(result.plan!!.perTransactionFeeAmount).isEqualByComparingTo(BigDecimal("0.100"))
            assertThat(result.plan!!.maxChargeAmount).isEqualByComparingTo(BigDecimal("500.000"))
            assertThat(result.plan!!.currency).isEqualTo("USD")
            assertThat(result.plan!!.billingCycle).isEqualTo("MONTHLY")
            assertThat(result.plan!!.status).isEqualTo("ACTIVE")
            assertThat(result.plan!!.effectiveFrom).isEqualTo(effectiveFrom)
            assertThat(result.plan!!.effectiveUntil).isEqualTo(effectiveUntil)
        }
    }

    @Nested
    @DisplayName("No active plan")
    inner class NoActivePlan {

        @Test
        fun `should return null plan when no active plan exists`() {
            val user = buildOAuthUserMock(merchantId)
            val query = GetMerchantBillingPlanQuery()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(billingPlanService.getActivePlan(merchantId)).thenReturn(null)

            val result = handler.handle(query)

            assertThat(result.plan).isNull()
        }
    }

    @Nested
    @DisplayName("User has no organization")
    inner class UserHasNoOrganization {

        @Test
        fun `should throw IllegalStateException when user has no organization`() {
            val user = buildOAuthUserMock(null)
            val query = GetMerchantBillingPlanQuery()

            whenever(userService.getCurrentUser()).thenReturn(user)

            assertThatThrownBy { handler.handle(query) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User has no organization")
        }
    }
}
