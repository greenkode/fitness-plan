package com.krachtix.audit.domain.query

import com.krachtix.audit.domain.model.AuditLog
import com.krachtix.audit.domain.model.PageRequest
import an.awesome.pipelinr.Command
import org.springframework.data.domain.Page

data class GetAuditLogsByMerchantQuery(
    val merchantId: String,
    val page: PageRequest,
) : Command<Page<AuditLog>>
