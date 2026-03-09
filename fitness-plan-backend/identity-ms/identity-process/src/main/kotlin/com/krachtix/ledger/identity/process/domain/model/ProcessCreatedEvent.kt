package com.krachtix.identity.process.domain.model

import com.krachtix.commons.process.enumeration.ProcessType
import java.util.UUID

data class ProcessCreatedEvent(val id: UUID, val processType: ProcessType, val expiry: Long? = null)