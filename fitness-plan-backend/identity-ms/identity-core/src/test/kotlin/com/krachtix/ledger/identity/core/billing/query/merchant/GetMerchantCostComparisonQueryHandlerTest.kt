package com.krachtix.identity.core.billing.query.merchant

import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.commons.billing.CostComparisonResponse
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
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetMerchantCostComparisonQueryHandlerTest {

    @Mock
    private lateinit var billingDataGateway: BillingDataGateway

    @Mock
    private lateinit var userService: UserService

    private lateinit var handler: GetMerchantCostComparisonQueryHandler

    private val merchantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = GetMerchantCostComparisonQueryHandler(billingDataGateway, userService)
    }

    private fun mockCurrentUser(organizationId: UUID?) {
        val user = Mockito.mock(OAuthUser::class.java)
        whenever(user.organizationId).thenReturn(organizationId)
        whenever(userService.getCurrentUser()).thenReturn(user)
    }

    @Nested
    @DisplayName("Successful Cost Comparison Retrieval")
    inner class SuccessfulRetrieval {

        @Test
        fun `should map all cost comparison fields correctly`() {
            mockCurrentUser(merchantId)

            val comparison = CostComparisonResponse(
                currentMonthCostCents = 7500L,
                previousMonthCostCents = 6000L,
                percentageChange = 25.0,
                forecastedMonthEndCostCents = 15000L,
                forecastedVsPreviousPercentage = 150.0
            )
            whenever(billingDataGateway.getCostComparison(merchantId)).thenReturn(comparison)

            val result = handler.handle(GetMerchantCostComparisonQuery())

            assertThat(result.response.currentMonthCostCents).isEqualTo(7500L)
            assertThat(result.response.previousMonthCostCents).isEqualTo(6000L)
            assertThat(result.response.percentageChange).isEqualTo(25.0)
            assertThat(result.response.forecastedMonthEndCostCents).isEqualTo(15000L)
            assertThat(result.response.forecastedVsPreviousPercentage).isEqualTo(150.0)
        }
    }

    @Nested
    @DisplayName("No Organization")
    inner class NoOrganization {

        @Test
        fun `should throw IllegalStateException when user has no organization`() {
            mockCurrentUser(null)

            assertThatThrownBy { handler.handle(GetMerchantCostComparisonQuery()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User has no organization")
        }
    }
}
