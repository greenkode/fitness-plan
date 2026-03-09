package com.krachtix.commons.model

import com.krachtix.commons.exception.ResponseCode
import com.krachtix.commons.process.enumeration.ProcessState
import java.time.LocalDateTime

class ErrorResponse(
    var message: String,
    val timestamp: String = LocalDateTime.now().toString(),
    var responseCode: String,
    val status: ProcessState,
    var reference: String? = null,
    var additionalInfo: Map<String, Any> = HashMap()
) {
    constructor(
        message: String,
        errorCode: ResponseCode,
        reference: String?,
        status: ProcessState,
        entry: Map<String, Any>
    ) : this(message, LocalDateTime.now().toString(), errorCode.code, status, reference, entry)

    constructor(
        errorCode: ResponseCode,
        reference: String?,
        status: ProcessState,
        entry: Map<String, Any>
    ) : this(errorCode.name, LocalDateTime.now().toString(), errorCode.code, status, reference, entry)
}
