package com.krachtix.identity.core.billing.query.merchant

import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.commons.billing.UsageSnapshotResponse
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
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetMerchantUsageHistoryQueryHandlerTest {

    @Mock
    private lateinit var billingDataGateway: BillingDataGateway

    @Mock
    private lateinit var userService: UserService

    private lateinit var handler: GetMerchantUsageHistoryQueryHandler

    private val merchantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = GetMerchantUsageHistoryQueryHandler(billingDataGateway, userService)
    }

    private fun mockCurrentUser(organizationId: UUID?) {
        val user = Mockito.mock(OAuthUser::class.java)
        whenever(user.organizationId).thenReturn(organizationId)
        whenever(userService.getCurrentUser()).thenReturn(user)
    }

    @Nested
    @DisplayName("Successful Usage History Retrieval")
    inner class SuccessfulRetrieval {

        @Test
        fun `should return snapshots with all fields mapped`() {
            mockCurrentUser(merchantId)

            val startDate = LocalDate.of(2026, 1, 1)
            val endDate = LocalDate.of(2026, 1, 31)

            val snapshots = listOf(
                UsageSnapshotResponse(
                    snapshotDate = LocalDate.of(2026, 1, 15),
                    activeAccountCount = 50,
                    transactionCount = 3200,
                    apiCallCount = 8500,
                    exportCount = 12,
                    webhookDeliveryCount = 450,
                    storageBytes = 1048576L
                ),
                UsageSnapshotResponse(
                    snapshotDate = LocalDate.of(2026, 1, 16),
                    activeAccountCount = 52,
                    transactionCount = 3350,
                    apiCallCount = 8700,
                    exportCount = 14,
                    webhookDeliveryCount = 460,
                    storageBytes = 1098000L
                )
            )
            whenever(billingDataGateway.getUsageHistory(merchantId, startDate, endDate)).thenReturn(snapshots)

            val result = handler.handle(GetMerchantUsageHistoryQuery(startDate, endDate))

            assertThat(result.response.snapshots).hasSize(2)
            assertThat(result.response.startDate).isEqualTo(startDate)
            assertThat(result.response.endDate).isEqualTo(endDate)

            val first = result.response.snapshots[0]
            assertThat(first.snapshotDate).isEqualTo(LocalDate.of(2026, 1, 15))
            assertThat(first.activeAccountCount).isEqualTo(50)
            assertThat(first.transactionCount).isEqualTo(3200)
            assertThat(first.apiCallCount).isEqualTo(8500)
            assertThat(first.exportCount).isEqualTo(12)
            assertThat(first.webhookDeliveryCount).isEqualTo(450)
            assertThat(first.storageBytes).isEqualTo(1048576L)
        }

        @Test
        fun `should return empty list when no snapshots`() {
            mockCurrentUser(merchantId)

            val startDate = LocalDate.of(2026, 2, 1)
            val endDate = LocalDate.of(2026, 2, 28)

            whenever(billingDataGateway.getUsageHistory(merchantId, startDate, endDate)).thenReturn(emptyList())

            val result = handler.handle(GetMerchantUsageHistoryQuery(startDate, endDate))

            assertThat(result.response.snapshots).isEmpty()
            assertThat(result.response.startDate).isEqualTo(startDate)
            assertThat(result.response.endDate).isEqualTo(endDate)
        }

        @Test
        fun `should pass date range to gateway`() {
            mockCurrentUser(merchantId)

            val startDate = LocalDate.of(2025, 6, 1)
            val endDate = LocalDate.of(2025, 12, 31)

            whenever(billingDataGateway.getUsageHistory(merchantId, startDate, endDate)).thenReturn(emptyList())

            handler.handle(GetMerchantUsageHistoryQuery(startDate, endDate))

            verify(billingDataGateway).getUsageHistory(merchantId, startDate, endDate)
        }
    }

    @Nested
    @DisplayName("No Organization")
    inner class NoOrganization {

        @Test
        fun `should throw IllegalStateException when user has no organization`() {
            mockCurrentUser(null)

            val startDate = LocalDate.of(2026, 1, 1)
            val endDate = LocalDate.of(2026, 1, 31)

            assertThatThrownBy { handler.handle(GetMerchantUsageHistoryQuery(startDate, endDate)) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User has no organization")
        }
    }
}
