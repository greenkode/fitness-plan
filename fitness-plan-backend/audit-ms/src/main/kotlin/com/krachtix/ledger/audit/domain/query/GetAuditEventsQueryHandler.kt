package com.krachtix.audit.domain.query

import com.krachtix.audit.domain.model.AuditLog
import an.awesome.pipelinr.Command
import com.krachtix.audit.domain.model.AuditLogEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetAuditEventsQueryHandler(private val auditLogReadRepository: AuditLogReadRepository) :
    Command.Handler<GetAuditLogsByMerchantQuery, Page<AuditLog>> {

    @Transactional(transactionManager = "readReplicaTransactionManager", readOnly = true)
    override fun handle(query: GetAuditLogsByMerchantQuery): Page<AuditLog> {

        val pageRequest = PageRequest.of(
            query.page.number, query.page.size, Sort.by(query.page.sort, AuditLogEntity.Companion.SORT_COLUMN)
        )

        return auditLogReadRepository.findAllByMerchantId(query.merchantId, pageRequest)
            .map { it.toDomain() }
    }
}
