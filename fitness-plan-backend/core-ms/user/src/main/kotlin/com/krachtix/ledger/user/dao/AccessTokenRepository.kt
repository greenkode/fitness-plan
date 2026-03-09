package com.krachtix.user.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface AccessTokenRepository : JpaRepository<AccessTokenEntity, Long> {

    fun findFirstByInstitutionAndResourceAndExpiryAfterOrderByExpiryDesc(
        institution: String,
        resource: String,
        date: Instant
    ): AccessTokenEntity?
}