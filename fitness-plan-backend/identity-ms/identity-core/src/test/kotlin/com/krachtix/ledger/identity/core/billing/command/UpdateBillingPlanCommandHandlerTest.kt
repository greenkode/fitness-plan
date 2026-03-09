package com.krachtix.identity.core.billing.command

import com.krachtix.identity.core.billing.dto.UpdateBillingPlanRequest
import com.krachtix.identity.core.billing.entity.BillingCycle
import com.krachtix.identity.core.billing.entity.BillingPlanEntity
import com.krachtix.identity.core.billing.entity.BillingPlanStatus
import com.krachtix.identity.core.billing.service.BillingPlanService
import com.krachtix.identity.core.organization.entity.Organization
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UpdateBillingPlanCommandHandlerTest {

    @Mock
    private lateinit var billingPlanService: BillingPlanService

    @Captor
    private lateinit var requestCaptor: ArgumentCaptor<UpdateBillingPlanRequest>

    private lateinit var handler: UpdateBillingPlanCommandHandler

    private val orgId = UUID.randomUUID()
    private val planPublicId = UUID.randomUUID()
    private val effectiveFrom = Instant.parse("2026-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        handler = UpdateBillingPlanCommandHandler(billingPlanService)
    }

    private fun buildOrganizationMock(): Organization {
        val organization = Mockito.mock(Organization::class.java)
        whenever(organization.id).thenReturn(orgId)
        return organization
    }

    private fun buildPlanEntity(
        organization: Organization,
        name: String = "Updated Plan"
    ): BillingPlanEntity = BillingPlanEntity(
        publicId = planPublicId,
        organization = organization,
        name = name,
        platformFeeAmount = BigDecimal("20.000"),
        perAccountFeeAmount = BigDecimal("0.750"),
        perTransactionFeeAmount = BigDecimal("0.150"),
        currency = "EUR",
        billingCycle = BillingCycle.QUARTERLY,
        status = BillingPlanStatus.ACTIVE,
        effectiveFrom = effectiveFrom
    )

    @Nested
    @DisplayName("Successful Plan Update")
    inner class SuccessfulUpdate {

        @Test
        fun `should update plan and return result`() {
            val organization = buildOrganizationMock()
            val entity = buildPlanEntity(organization)

            val command = UpdateBillingPlanCommand(
                publicId = planPublicId,
                name = "Updated Plan",
                platformFeeAmount = BigDecimal("20.000"),
                perAccountFeeAmount = BigDecimal("0.750"),
                perTransactionFeeAmount = BigDecimal("0.150"),
                currency = "EUR",
                billingCycle = BillingCycle.QUARTERLY
            )

            whenever(billingPlanService.updatePlan(eq(planPublicId), capture(requestCaptor))).thenReturn(entity)

            val result = handler.handle(command)

            assertThat(result.publicId).isEqualTo(planPublicId)
            assertThat(result.name).isEqualTo("Updated Plan")
            assertThat(result.status).isEqualTo("ACTIVE")
        }

        @Test
        fun `should pass partial update with only name`() {
            val organization = buildOrganizationMock()
            val entity = buildPlanEntity(organization, name = "Renamed Plan")

            val command = UpdateBillingPlanCommand(
                publicId = planPublicId,
                name = "Renamed Plan"
            )

            whenever(billingPlanService.updatePlan(eq(planPublicId), capture(requestCaptor))).thenReturn(entity)

            handler.handle(command)

            val captured = requestCaptor.value
            assertThat(captured.name).isEqualTo("Renamed Plan")
            assertThat(captured.platformFeeAmount).isNull()
            assertThat(captured.perAccountFeeAmount).isNull()
            assertThat(captured.perTransactionFeeAmount).isNull()
            assertThat(captured.currency).isNull()
            assertThat(captured.billingCycle).isNull()
            assertThat(captured.effectiveUntil).isNull()
        }

        @Test
        fun `should pass all fields update`() {
            val organization = buildOrganizationMock()
            val entity = buildPlanEntity(organization)
            val newEffectiveUntil = Instant.parse("2027-06-30T23:59:59Z")

            val command = UpdateBillingPlanCommand(
                publicId = planPublicId,
                name = "Updated Plan",
                platformFeeAmount = BigDecimal("20.000"),
                perAccountFeeAmount = BigDecimal("0.750"),
                perTransactionFeeAmount = BigDecimal("0.150"),
                currency = "EUR",
                billingCycle = BillingCycle.QUARTERLY,
                effectiveUntil = newEffectiveUntil
            )

            whenever(billingPlanService.updatePlan(eq(planPublicId), capture(requestCaptor))).thenReturn(entity)

            handler.handle(command)

            val captured = requestCaptor.value
            assertThat(captured.name).isEqualTo("Updated Plan")
            assertThat(captured.platformFeeAmount).isEqualTo(BigDecimal("20.000"))
            assertThat(captured.perAccountFeeAmount).isEqualTo(BigDecimal("0.750"))
            assertThat(captured.perTransactionFeeAmount).isEqualTo(BigDecimal("0.150"))
            assertThat(captured.currency).isEqualTo("EUR")
            assertThat(captured.billingCycle).isEqualTo(BillingCycle.QUARTERLY)
            assertThat(captured.effectiveUntil).isEqualTo(newEffectiveUntil)
        }
    }
}
