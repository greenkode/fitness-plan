package com.krachtix.identity.core.repository

import com.krachtix.commons.dto.Email
import com.krachtix.identity.core.entity.OAuthUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OAuthUserRepository : JpaRepository<OAuthUser, UUID> {
    fun findByUsername(username: String): OAuthUser?

    fun findByEmail(email: Email): OAuthUser?
    fun findByMerchantId(merchantId: UUID): List<OAuthUser>
    fun findByOrganizationId(organizationId: UUID): List<OAuthUser>
    fun existsByEmail(email: Email): Boolean

    @Query("SELECT u FROM OAuthUser u JOIN u.authorities a WHERE u.merchantId = :merchantId AND a = 'ROLE_SUPER_ADMIN'")
    fun findSuperAdminsByMerchantId(merchantId: UUID): List<OAuthUser>

    @Query("SELECT u FROM OAuthUser u LEFT JOIN FETCH u.settings WHERE u.id = :id")
    fun findByIdWithSettings(id: UUID): OAuthUser?

    @Query("SELECT u FROM OAuthUser u LEFT JOIN FETCH u.settings WHERE u.username = :username")
    fun findByUsernameWithSettings(username: String): OAuthUser?
}