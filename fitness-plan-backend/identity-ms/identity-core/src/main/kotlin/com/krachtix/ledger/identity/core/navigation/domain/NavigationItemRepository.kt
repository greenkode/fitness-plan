package com.krachtix.identity.core.navigation.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface NavigationItemRepository : JpaRepository<NavigationItemEntity, Int> {

    @Query("""
        SELECT DISTINCT ni FROM NavigationItemEntity ni
        LEFT JOIN FETCH ni.roles
        WHERE ni.active = true
          AND ni.platform = :platform
          AND (ni.path IS NULL OR EXISTS (
              SELECT 1 FROM NavigationItemEntity r
              LEFT JOIN r.roles role
              WHERE r.id = ni.id AND role IN :roles
          ))
        ORDER BY ni.sortOrder
    """)
    fun findAccessibleItems(
        @Param("platform") platform: String,
        @Param("roles") roles: Set<String>
    ): List<NavigationItemEntity>
}
