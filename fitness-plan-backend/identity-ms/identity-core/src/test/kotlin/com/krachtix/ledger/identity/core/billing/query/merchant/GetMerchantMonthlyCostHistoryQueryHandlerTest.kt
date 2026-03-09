package com.krachtix.identity.core.billing.query.merchant

import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.commons.billing.MonthlyCostResponse
import com.krachtix.identity.core.billing.entity.BillingPlanEntity
import com.krachtix.identity.core.billing.service.BillingPlanService
import com.krachtix.identity.core.entity.OAuthUser
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetMerchantMonthlyCostHistoryQueryHandlerTest {

    @Mock
    private lateinit var billingDataGateway: BillingDataGateway

    @Mock
    private lateinit var billingPlanService: BillingPlanService

    @Mock
    private lateinit var userService: UserService

    private lateinit var handler: GetMerchantMonthlyCostHistoryQueryHandler

    private val merchantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = GetMerchantMonthlyCostHistoryQueryHandler(billingDataGateway, billingPlanService, userService)
    }

    private fun mockCurrentUser(organizationId: UUID?) {
        val user = Mockito.mock(OAuthUser::class.java)
        whenever(user.organizationId).thenReturn(organizationId)
        whenever(userService.getCurrentUser()).thenReturn(user)
    }

    @Nested
    @DisplayName("Successful Cost History Retrieval")
    inner class SuccessfulRetrieval {

        @Test
        fun `should return cost history with mapped fields`() {
            mockCurrentUser(merchantId)

            val history = listOf(
                MonthlyCostResponse(
                    yearMonth = "2026-01",
                    platformFeeCents = 1000L,
                    accountFeeCents = 2500L,
                    transactionFeeCents = 3000L,
                    totalCents = 6500L
                ),
                MonthlyCostResponse(
                    yearMonth = "2025-12",
                    platformFeeCents = 900L,
                    accountFeeCents = 2200L,
                    transactionFeeCents = 2800L,
                    totalCents = 5900L
                )
            )
            whenever(billingDataGateway.getCostHistory(merchantId, 6)).thenReturn(history)

            val plan = Mockito.mock(BillingPlanEntity::class.java)
            whenever(plan.currency).thenReturn("EUR")
            whenever(billingPlanService.getActivePlan(merchantId)).thenReturn(plan)

            val result = handler.handle(GetMerchantMonthlyCostHistoryQuery())

            assertThat(result.response.history).hasSize(2)
            assertThat(result.response.history[0].yearMonth).isEqualTo("2026-01")
            assertThat(result.response.history[0].platformFeeCents).isEqualTo(1000L)
            assertThat(result.response.history[0].accountFeeCents).isEqualTo(2500L)
            assertThat(result.response.history[0].transactionFeeCents).isEqualTo(3000L)
            assertThat(result.response.history[0].totalCents).isEqualTo(6500L)
            assertThat(result.response.history[1].yearMonth).isEqualTo("2025-12")
            assertThat(result.response.history[1].platformFeeCents).isEqualTo(900L)
            assertThat(result.response.history[1].accountFeeCents).isEqualTo(2200L)
            assertThat(result.response.history[1].transactionFeeCents).isEqualTo(2800L)
            assertThat(result.response.history[1].totalCents).isEqualTo(5900L)
        }

        @Test
        fun `should use plan currency when plan exists`() {
            mockCurrentUser(merchantId)

            whenever(billingDataGateway.getCostHistory(merchantId, 6)).thenReturn(emptyList())

            val plan = Mockito.mock(BillingPlanEntity::class.java)
            whenever(plan.currency).thenReturn("GBP")
            whenever(billingPlanService.getActivePlan(merchantId)).thenReturn(plan)

            val result = handler.handle(GetMerchantMonthlyCostHistoryQuery())

            assertThat(result.response.currency).isEqualTo("GBP")
        }

        @Test
        fun `should default to USD when no active plan`() {
            mockCurrentUser(merchantId)

            whenever(billingDataGateway.getCostHistory(merchantId, 6)).thenReturn(emptyList())
            whenever(billingPlanService.getActivePlan(merchantId)).thenReturn(null)

            val result = handler.handle(GetMerchantMonthlyCostHistoryQuery())

            assertThat(result.response.currency).isEqualTo("USD")
        }

        @Test
        fun `should pass months param to gateway`() {
            mockCurrentUser(merchantId)

            whenever(billingDataGateway.getCostHistory(merchantId, 12)).thenReturn(emptyList())
            whenever(billingPlanService.getActivePlan(merchantId)).thenReturn(null)

            handler.handle(GetMerchantMonthlyCostHistoryQuery(months = 12))

            verify(billingDataGateway).getCostHistory(merchantId, 12)
        }
    }

    @Nested
    @DisplayName("No Organization")
    inner class NoOrganization {

        @Test
        fun `should throw IllegalStateException when user has no organization`() {
            mockCurrentUser(null)

            assertThatThrownBy { handler.handle(GetMerchantMonthlyCostHistoryQuery()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User has no organization")
        }
    }
}
