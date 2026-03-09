package com.krachtix.identity.core.billing.query

import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.billing.entity.BillingCycle
import com.krachtix.identity.core.billing.entity.BillingPlanEntity
import com.krachtix.identity.core.billing.entity.BillingPlanStatus
import com.krachtix.identity.core.billing.service.BillingPlanService
import com.krachtix.identity.core.organization.entity.Organization
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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetBillingPlanQueryHandlerTest {

    @Mock
    private lateinit var billingPlanService: BillingPlanService

    @Mock
    private lateinit var messageService: MessageService

    private lateinit var handler: GetBillingPlanQueryHandler

    private val orgId = UUID.randomUUID()
    private val planPublicId = UUID.randomUUID()
    private val effectiveFrom = Instant.parse("2026-01-01T00:00:00Z")
    private val effectiveUntil = Instant.parse("2026-12-31T23:59:59Z")
    private val createdAt = Instant.parse("2025-12-15T10:00:00Z")

    @BeforeEach
    fun setUp() {
        handler = GetBillingPlanQueryHandler(billingPlanService, messageService)
    }

    private fun buildOrganizationMock(): Organization {
        val organization = Mockito.mock(Organization::class.java)
        whenever(organization.id).thenReturn(orgId)
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
            currency = "USD",
            billingCycle = BillingCycle.MONTHLY,
            status = BillingPlanStatus.ACTIVE,
            effectiveFrom = effectiveFrom,
            effectiveUntil = effectiveUntil
        ).apply {
            this.createdAt = this@GetBillingPlanQueryHandlerTest.createdAt
        }

    @Nested
    @DisplayName("Successful Plan Retrieval")
    inner class SuccessfulRetrieval {

        @Test
        fun `should return plan when found`() {
            val organization = buildOrganizationMock()
            val entity = buildPlanEntity(organization)
            val query = GetBillingPlanQuery(publicId = planPublicId)

            whenever(billingPlanService.getPlan(planPublicId)).thenReturn(entity)

            val result = handler.handle(query)

            assertThat(result.plan.publicId).isEqualTo(planPublicId)
            assertThat(result.plan.organizationId).isEqualTo(orgId)
            assertThat(result.plan.name).isEqualTo("Standard Plan")
            assertThat(result.plan.platformFeeAmount).isEqualTo(BigDecimal("10.000"))
            assertThat(result.plan.perAccountFeeAmount).isEqualTo(BigDecimal("0.500"))
            assertThat(result.plan.perTransactionFeeAmount).isEqualTo(BigDecimal("0.100"))
            assertThat(result.plan.currency).isEqualTo("USD")
            assertThat(result.plan.billingCycle).isEqualTo(BillingCycle.MONTHLY)
            assertThat(result.plan.status).isEqualTo(BillingPlanStatus.ACTIVE)
            assertThat(result.plan.effectiveFrom).isEqualTo(effectiveFrom)
            assertThat(result.plan.effectiveUntil).isEqualTo(effectiveUntil)
            assertThat(result.plan.createdAt).isEqualTo(createdAt)
        }
    }

    @Nested
    @DisplayName("Plan Not Found")
    inner class PlanNotFound {

        @Test
        fun `should throw RecordNotFoundException when plan not found`() {
            val query = GetBillingPlanQuery(publicId = planPublicId)

            whenever(billingPlanService.getPlan(planPublicId)).thenReturn(null)
            whenever(messageService.getMessage("billing.error.plan_not_found")).thenReturn("Billing plan not found")

            assertThatThrownBy { handler.handle(query) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Billing plan not found")
        }

        @Test
        fun `should use message service for error message`() {
            val query = GetBillingPlanQuery(publicId = planPublicId)

            whenever(billingPlanService.getPlan(planPublicId)).thenReturn(null)
            whenever(messageService.getMessage("billing.error.plan_not_found")).thenReturn("Billing plan not found")

            assertThatThrownBy { handler.handle(query) }
                .isInstanceOf(RecordNotFoundException::class.java)

            verify(messageService).getMessage("billing.error.plan_not_found")
        }
    }
}
