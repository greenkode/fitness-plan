package com.krachtix.commons.audit

import java.time.Instant

data class AuditEvent(
    val identity: String,
    val identityType: IdentityType,
    val resource: AuditResource,
    val event: EventType,
    val eventTime: Instant,
    val timeRecorded: Instant,
    val payload: String,
)

