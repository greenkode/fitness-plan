package com.krachtix.identity.core.totp.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TotpRecoveryCodeRepository : JpaRepository<TotpRecoveryCodeEntity, UUID> {

    fun findByUserIdAndUsedFalse(userId: UUID): List<TotpRecoveryCodeEntity>

    fun deleteByUserId(userId: UUID)

    fun countByUserIdAndUsedFalse(userId: UUID): Long
}
