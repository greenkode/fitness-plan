//package com.krachtix.audit.listener
//
//import com.krachtix.audit.domain.model.AuditLog
//
//fun AuditLogEvent.toDomain(): com.krachtix.audit.AuditLog {
//    return AuditLog(
//        id,
//        identity.toString(),
//        identityType.toString(),
//        resource.toString(),
//        event.toString(),
//        eventTime,
//        timeRecorded,
//        payload.toString()
//    )
//}
//
