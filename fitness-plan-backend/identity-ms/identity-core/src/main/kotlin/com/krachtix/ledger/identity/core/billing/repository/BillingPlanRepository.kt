package com.krachtix.identity.core.billing.repository

import com.krachtix.identity.core.billing.entity.BillingPlanEntity
import com.krachtix.identity.core.billing.entity.BillingPlanStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BillingPlanRepository : JpaRepository<BillingPlanEntity, UUID> {
    fun findByOrganizationIdAndStatus(organizationId: UUID, status: BillingPlanStatus): BillingPlanEntity?
    fun findByPublicId(publicId: UUID): BillingPlanEntity?
    fun findAllByStatus(status: BillingPlanStatus, pageable: Pageable): Page<BillingPlanEntity>
    fun findAllByOrganizationId(organizationId: UUID): List<BillingPlanEntity>
    fun findAllByOrganizationIdAndStatus(organizationId: UUID, status: BillingPlanStatus, pageable: Pageable): Page<BillingPlanEntity>
}
