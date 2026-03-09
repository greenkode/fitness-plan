package com.krachtix.identity.core.billing.query

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
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ListBillingPlansQueryHandlerTest {

    @Mock
    private lateinit var billingPlanService: BillingPlanService

    private lateinit var handler: ListBillingPlansQueryHandler

    private val orgId = UUID.randomUUID()
    private val effectiveFrom = Instant.parse("2026-01-01T00:00:00Z")
    private val createdAt = Instant.parse("2025-12-15T10:00:00Z")

    @BeforeEach
    fun setUp() {
        handler = ListBillingPlansQueryHandler(billingPlanService)
    }

    private fun buildOrganizationMock(): Organization {
        val organization = Mockito.mock(Organization::class.java)
        whenever(organization.id).thenReturn(orgId)
        return organization
    }

    private fun buildPlanEntity(
        name: String,
        status: BillingPlanStatus = BillingPlanStatus.ACTIVE,
        organization: Organization
    ): BillingPlanEntity = BillingPlanEntity(
        publicId = UUID.randomUUID(),
        organization = organization,
        name = name,
        platformFeeAmount = BigDecimal("10.000"),
        perAccountFeeAmount = BigDecimal("0.500"),
        perTransactionFeeAmount = BigDecimal("0.100"),
        currency = "USD",
        billingCycle = BillingCycle.MONTHLY,
        status = status,
        effectiveFrom = effectiveFrom
    ).apply {
        this.createdAt = this@ListBillingPlansQueryHandlerTest.createdAt
    }

    @Nested
    @DisplayName("Successful Plan Listing")
    inner class SuccessfulListing {

        @Test
        fun `should return paginated plans`() {
            val organization = buildOrganizationMock()
            val entity1 = buildPlanEntity("Plan A", organization = organization)
            val entity2 = buildPlanEntity("Plan B", organization = organization)

            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(entity1, entity2), pageable, 2)

            val query = ListBillingPlansQuery(page = 0, size = 20)

            whenever(billingPlanService.listPlans(null, null, pageable)).thenReturn(page)

            val result = handler.handle(query)

            assertThat(result.response.plans).hasSize(2)
            assertThat(result.response.plans[0].name).isEqualTo("Plan A")
            assertThat(result.response.plans[1].name).isEqualTo("Plan B")
            assertThat(result.response.page).isEqualTo(0)
            assertThat(result.response.size).isEqualTo(20)
            assertThat(result.response.totalElements).isEqualTo(2)
            assertThat(result.response.totalPages).isEqualTo(1)
        }

        @Test
        fun `should filter by organizationId`() {
            val organization = buildOrganizationMock()
            val entity = buildPlanEntity("Org Plan", organization = organization)

            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(entity), pageable, 1)

            val query = ListBillingPlansQuery(organizationId = orgId, page = 0, size = 20)

            whenever(billingPlanService.listPlans(orgId, null, pageable)).thenReturn(page)

            val result = handler.handle(query)

            assertThat(result.response.plans).hasSize(1)
            assertThat(result.response.plans[0].organizationId).isEqualTo(orgId)

            verify(billingPlanService).listPlans(orgId, null, pageable)
        }

        @Test
        fun `should filter by status`() {
            val organization = buildOrganizationMock()
            val entity = buildPlanEntity("Cancelled Plan", status = BillingPlanStatus.CANCELLED, organization = organization)

            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(entity), pageable, 1)

            val query = ListBillingPlansQuery(status = BillingPlanStatus.CANCELLED, page = 0, size = 20)

            whenever(billingPlanService.listPlans(null, BillingPlanStatus.CANCELLED, pageable)).thenReturn(page)

            val result = handler.handle(query)

            assertThat(result.response.plans).hasSize(1)
            assertThat(result.response.plans[0].status).isEqualTo(BillingPlanStatus.CANCELLED)

            verify(billingPlanService).listPlans(null, BillingPlanStatus.CANCELLED, pageable)
        }

        @Test
        fun `should return empty page when no plans exist`() {
            val pageable = PageRequest.of(0, 20)
            val emptyPage = PageImpl<BillingPlanEntity>(emptyList(), pageable, 0)

            val query = ListBillingPlansQuery(page = 0, size = 20)

            whenever(billingPlanService.listPlans(null, null, pageable)).thenReturn(emptyPage)

            val result = handler.handle(query)

            assertThat(result.response.plans).isEmpty()
            assertThat(result.response.totalElements).isEqualTo(0)
            assertThat(result.response.totalPages).isEqualTo(0)
        }

        @Test
        fun `should pass correct page parameters`() {
            val pageable = PageRequest.of(3, 5)
            val emptyPage = PageImpl<BillingPlanEntity>(emptyList(), pageable, 0)

            val query = ListBillingPlansQuery(page = 3, size = 5)

            whenever(billingPlanService.listPlans(null, null, pageable)).thenReturn(emptyPage)

            handler.handle(query)

            verify(billingPlanService).listPlans(null, null, pageable)
        }
    }
}
