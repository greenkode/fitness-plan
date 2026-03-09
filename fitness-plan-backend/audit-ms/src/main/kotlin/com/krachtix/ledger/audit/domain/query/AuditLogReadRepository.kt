package com.krachtix.audit.domain.query

import com.krachtix.audit.domain.model.AuditLogEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface AuditLogReadRepository : CrudRepository<AuditLogEntity, UUID> {

    fun findAllByMerchantId(
        merchantId: String,
        pageable: Pageable
    ): Page<AuditLogEntity>
}