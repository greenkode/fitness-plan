package com.krachtix.identity.core.organization.repository

import com.krachtix.identity.core.entity.OrganizationStatus
import com.krachtix.identity.core.organization.entity.Organization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface OrganizationRepository : JpaRepository<Organization, UUID> {

    fun findBySlug(slug: String): Organization?

    fun findByIdAndStatus(id: UUID, status: OrganizationStatus): Organization?

    fun existsBySlug(slug: String): Boolean

    fun existsByNameIgnoreCase(name: String): Boolean

    fun findAllByStatus(status: OrganizationStatus): List<Organization>

    @Query("SELECT o FROM Organization o LEFT JOIN FETCH o.properties WHERE o.id = :id")
    fun findByIdWithProperties(id: UUID): Optional<Organization>
}
