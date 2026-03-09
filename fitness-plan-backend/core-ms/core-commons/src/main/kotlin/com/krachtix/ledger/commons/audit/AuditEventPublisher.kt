package com.krachtix.commons.audit

interface AuditEventPublisher {
    fun publish(auditEvent: AuditEvent)
}


