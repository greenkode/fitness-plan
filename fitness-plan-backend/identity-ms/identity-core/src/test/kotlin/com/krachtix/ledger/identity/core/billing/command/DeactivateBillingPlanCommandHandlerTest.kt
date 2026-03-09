package com.krachtix.identity.core.billing.command

import com.krachtix.identity.core.billing.entity.BillingCycle
import com.krachtix.identity.core.billing.entity.BillingPlanEntity
import com.krachtix.identity.core.billing.entity.BillingPlanStatus
import com.krachtix.identity.core.billing.service.BillingPlanService
import com.krachtix.identity.core.organization.entity.Organization
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
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
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class DeactivateBillingPlanCommandHandlerTest {

    @Mock
    private lateinit var billingPlanService: BillingPlanService

    private lateinit var handler: DeactivateBillingPlanCommandHandler

    private val orgId = UUID.randomUUID()
    private val planPublicId = UUID.randomUUID()
    private val effectiveFrom = Instant.parse("2026-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        handler = DeactivateBillingPlanCommandHandler(billingPlanService)
    }

    private fun buildOrganizationMock(): Organization {
        val organization = Mockito.mock(Organization::class.java)
        whenever(organization.id).thenReturn(orgId)
        return organization
    }

    @Nested
    @DisplayName("Successful Plan Deactivation")
    inner class SuccessfulDeactivation {

        @Test
        fun `should deactivate plan and return CANCELLED status`() {
            val organization = buildOrganizationMock()
            val entity = BillingPlanEntity(
                publicId = planPublicId,
                organization = organization,
                name = "Standard Plan",
                platformFeeAmount = BigDecimal("10.000"),
                perAccountFeeAmount = BigDecimal("0.500"),
                perTransactionFeeAmount = BigDecimal("0.100"),
                currency = "USD",
                billingCycle = BillingCycle.MONTHLY,
                status = BillingPlanStatus.CANCELLED,
                effectiveFrom = effectiveFrom,
                effectiveUntil = Instant.now()
            )

            val command = DeactivateBillingPlanCommand(publicId = planPublicId)

            whenever(billingPlanService.deactivatePlan(planPublicId)).thenReturn(entity)

            val result = handler.handle(command)

            assertThat(result.publicId).isEqualTo(planPublicId)
            assertThat(result.status).isEqualTo("CANCELLED")
        }

        @Test
        fun `should call service with correct publicId`() {
            val organization = buildOrganizationMock()
            val entity = BillingPlanEntity(
                publicId = planPublicId,
                organization = organization,
                name = "Standard Plan",
                platformFeeAmount = BigDecimal("10.000"),
                perAccountFeeAmount = BigDecimal("0.500"),
                perTransactionFeeAmount = BigDecimal("0.100"),
                currency = "USD",
                billingCycle = BillingCycle.MONTHLY,
                status = BillingPlanStatus.CANCELLED,
                effectiveFrom = effectiveFrom,
                effectiveUntil = Instant.now()
            )

            val command = DeactivateBillingPlanCommand(publicId = planPublicId)

            whenever(billingPlanService.deactivatePlan(planPublicId)).thenReturn(entity)

            handler.handle(command)

            verify(billingPlanService).deactivatePlan(planPublicId)
        }
    }
}
