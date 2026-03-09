package com.krachtix.identity.core.billing.query.merchant

import com.krachtix.identity.commons.billing.BillingDataGateway
import com.krachtix.identity.commons.billing.BillingProjectionResponse
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
class GetMerchantBillingProjectionQueryHandlerTest {

    @Mock
    private lateinit var billingDataGateway: BillingDataGateway

    @Mock
    private lateinit var userService: UserService

    private lateinit var handler: GetMerchantBillingProjectionQueryHandler

    private val merchantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = GetMerchantBillingProjectionQueryHandler(billingDataGateway, userService)
    }

    private fun mockCurrentUser(organizationId: UUID?) {
        val user = Mockito.mock(OAuthUser::class.java)
        whenever(user.organizationId).thenReturn(organizationId)
        whenever(userService.getCurrentUser()).thenReturn(user)
    }

    @Nested
    @DisplayName("Successful Projection Retrieval")
    inner class SuccessfulRetrieval {

        @Test
        fun `should map all projection fields correctly`() {
            mockCurrentUser(merchantId)

            val projection = BillingProjectionResponse(
                projectedAccountFeeCents = 2500L,
                projectedTransactionFeeCents = 3000L,
                platformFeeCents = 1000L,
                projectedTotalCents = 6500L
            )
            whenever(billingDataGateway.getProjection(merchantId)).thenReturn(projection)

            val result = handler.handle(GetMerchantBillingProjectionQuery())

            assertThat(result.projection.projectedAccountFeeCents).isEqualTo(2500L)
            assertThat(result.projection.projectedTransactionFeeCents).isEqualTo(3000L)
            assertThat(result.projection.platformFeeCents).isEqualTo(1000L)
            assertThat(result.projection.projectedTotalCents).isEqualTo(6500L)
        }
    }

    @Nested
    @DisplayName("No Organization")
    inner class NoOrganization {

        @Test
        fun `should throw IllegalStateException when user has no organization`() {
            mockCurrentUser(null)

            assertThatThrownBy { handler.handle(GetMerchantBillingProjectionQuery()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User has no organization")
        }
    }
}
