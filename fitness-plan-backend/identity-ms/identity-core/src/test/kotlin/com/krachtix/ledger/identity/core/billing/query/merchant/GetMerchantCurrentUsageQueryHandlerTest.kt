package com.krachtix.identity.core.billing.query.merchant

import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.commons.billing.CurrentUsageResponse
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
class GetMerchantCurrentUsageQueryHandlerTest {

    @Mock
    private lateinit var billingDataGateway: BillingDataGateway

    @Mock
    private lateinit var userService: UserService

    private lateinit var handler: GetMerchantCurrentUsageQueryHandler

    private val merchantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = GetMerchantCurrentUsageQueryHandler(billingDataGateway, userService)
    }

    private fun mockCurrentUser(organizationId: UUID?) {
        val user = Mockito.mock(OAuthUser::class.java)
        whenever(user.organizationId).thenReturn(organizationId)
        whenever(userService.getCurrentUser()).thenReturn(user)
    }

    @Nested
    @DisplayName("Successful Usage Retrieval")
    inner class SuccessfulRetrieval {

        @Test
        fun `should map all usage fields correctly`() {
            mockCurrentUser(merchantId)

            val usage = CurrentUsageResponse(
                activeAccounts = 25,
                transactionCount = 1500,
                apiCallCount = 4200,
                estimatedCostCents = 7500L,
                daysElapsed = 15,
                daysInPeriod = 30
            )
            whenever(billingDataGateway.getCurrentUsage(merchantId)).thenReturn(usage)

            val result = handler.handle(GetMerchantCurrentUsageQuery())

            assertThat(result.usage.activeAccounts).isEqualTo(25)
            assertThat(result.usage.transactionCount).isEqualTo(1500)
            assertThat(result.usage.apiCallCount).isEqualTo(4200)
            assertThat(result.usage.estimatedCostCents).isEqualTo(7500L)
            assertThat(result.usage.daysElapsed).isEqualTo(15)
            assertThat(result.usage.daysInPeriod).isEqualTo(30)
        }
    }

    @Nested
    @DisplayName("No Organization")
    inner class NoOrganization {

        @Test
        fun `should throw IllegalStateException when user has no organization`() {
            mockCurrentUser(null)

            assertThatThrownBy { handler.handle(GetMerchantCurrentUsageQuery()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User has no organization")
        }
    }
}
