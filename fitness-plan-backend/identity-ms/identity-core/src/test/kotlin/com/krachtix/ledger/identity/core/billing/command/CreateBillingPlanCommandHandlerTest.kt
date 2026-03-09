package com.krachtix.identity.core.billing.command

import com.krachtix.identity.core.billing.dto.CreateBillingPlanRequest
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
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CreateBillingPlanCommandHandlerTest {

    @Mock
    private lateinit var billingPlanService: BillingPlanService

    @Captor
    private lateinit var requestCaptor: ArgumentCaptor<CreateBillingPlanRequest>

    private lateinit var handler: CreateBillingPlanCommandHandler

    private val orgId = UUID.randomUUID()
    private val planPublicId = UUID.randomUUID()
    private val effectiveFrom = Instant.parse("2026-01-01T00:00:00Z")
    private val effectiveUntil = Instant.parse("2026-12-31T23:59:59Z")

    @BeforeEach
    fun setUp() {
        handler = CreateBillingPlanCommandHandler(billingPlanService)
    }

    private fun buildOrganizationMock(): Organization {
        val organization = Mockito.mock(Organization::class.java)
        whenever(organization.id).thenReturn(orgId)
        return organization
    }

    private fun buildPlanEntity(
        organization: Organization,
        name: String = "Standard Plan",
        effectiveUntilValue: Instant? = effectiveUntil
    ): BillingPlanEntity = BillingPlanEntity(
        publicId = planPublicId,
        organization = organization,
        name = name,
        platformFeeAmount = BigDecimal("10.000"),
        perAccountFeeAmount = BigDecimal("0.500"),
        perTransactionFeeAmount = BigDecimal("0.100"),
        currency = "USD",
        billingCycle = BillingCycle.MONTHLY,
        status = BillingPlanStatus.ACTIVE,
        effectiveFrom = effectiveFrom,
        effectiveUntil = effectiveUntilValue
    )

    @Nested
    @DisplayName("Successful Plan Creation")
    inner class SuccessfulCreation {

        @Test
        fun `should create plan and return result with correct fields`() {
            val organization = buildOrganizationMock()
            val entity = buildPlanEntity(organization)

            val command = CreateBillingPlanCommand(
                organizationId = orgId,
                name = "Standard Plan",
                platformFeeAmount = BigDecimal("10.000"),
                perAccountFeeAmount = BigDecimal("0.500"),
                perTransactionFeeAmount = BigDecimal("0.100"),
                currency = "USD",
                billingCycle = BillingCycle.MONTHLY,
                effectiveFrom = effectiveFrom,
                effectiveUntil = effectiveUntil
            )

            whenever(billingPlanService.createPlan(org.mockito.kotlin.any())).thenReturn(entity)

            val result = handler.handle(command)

            assertThat(result.publicId).isEqualTo(planPublicId)
            assertThat(result.organizationId).isEqualTo(orgId)
            assertThat(result.name).isEqualTo("Standard Plan")
            assertThat(result.status).isEqualTo("ACTIVE")
        }

        @Test
        fun `should pass correct request to service`() {
            val organization = buildOrganizationMock()
            val entity = buildPlanEntity(organization)

            val command = CreateBillingPlanCommand(
                organizationId = orgId,
                name = "Standard Plan",
                platformFeeAmount = BigDecimal("10.000"),
                perAccountFeeAmount = BigDecimal("0.500"),
                perTransactionFeeAmount = BigDecimal("0.100"),
                currency = "USD",
                billingCycle = BillingCycle.MONTHLY,
                effectiveFrom = effectiveFrom,
                effectiveUntil = effectiveUntil
            )

            whenever(billingPlanService.createPlan(org.mockito.kotlin.capture(requestCaptor))).thenReturn(entity)

            handler.handle(command)

            val captured = requestCaptor.value
            assertThat(captured.organizationId).isEqualTo(orgId)
            assertThat(captured.name).isEqualTo("Standard Plan")
            assertThat(captured.platformFeeAmount).isEqualTo(BigDecimal("10.000"))
            assertThat(captured.perAccountFeeAmount).isEqualTo(BigDecimal("0.500"))
            assertThat(captured.perTransactionFeeAmount).isEqualTo(BigDecimal("0.100"))
            assertThat(captured.currency).isEqualTo("USD")
            assertThat(captured.billingCycle).isEqualTo(BillingCycle.MONTHLY)
            assertThat(captured.effectiveFrom).isEqualTo(effectiveFrom)
            assertThat(captured.effectiveUntil).isEqualTo(effectiveUntil)
        }

        @Test
        fun `should handle null effectiveUntil`() {
            val organization = buildOrganizationMock()
            val entity = buildPlanEntity(organization, effectiveUntilValue = null)

            val command = CreateBillingPlanCommand(
                organizationId = orgId,
                name = "Standard Plan",
                platformFeeAmount = BigDecimal("10.000"),
                perAccountFeeAmount = BigDecimal("0.500"),
                perTransactionFeeAmount = BigDecimal("0.100"),
                currency = "USD",
                billingCycle = BillingCycle.MONTHLY,
                effectiveFrom = effectiveFrom,
                effectiveUntil = null
            )

            whenever(billingPlanService.createPlan(org.mockito.kotlin.capture(requestCaptor))).thenReturn(entity)

            val result = handler.handle(command)

            assertThat(requestCaptor.value.effectiveUntil).isNull()
            assertThat(result.publicId).isEqualTo(planPublicId)
            assertThat(result.status).isEqualTo("ACTIVE")
        }
    }
}
