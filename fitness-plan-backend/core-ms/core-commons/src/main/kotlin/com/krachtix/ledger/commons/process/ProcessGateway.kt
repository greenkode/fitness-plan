package com.krachtix.commons.process

import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.UUID

const val PROCESS_ERROR = "process_error"

interface ProcessGateway {

    fun createProcess(payload: CreateNewProcessPayload): ProcessDto

    fun findByPublicId(id: UUID): ProcessDto?

    fun findByTypeAndExternalReference(type: ProcessType, externalReference: String): ProcessDto?

    fun findByExternalReference(externalReference: String): ProcessDto?

    fun findByTypesAndExternalReference(types: Set<ProcessType>, externalReference: String): ProcessDto?

    fun makeRequest(payload: MakeProcessRequestPayload)

    fun completeProcess(processId: UUID, requestId: Long)

    fun updateProcessRequestState(processId: UUID, requestId: Long, state: ProcessState)

    fun failProcess(publicId: UUID)

    fun expireProcess(id: UUID, internal: Boolean)

    fun updateProcessState(publicId: UUID, newState: ProcessState)

    fun findPendingProcessByPublicId(id: UUID): ProcessDto?

    fun findPendingProcessByExternalReference(externalReference: String): ProcessDto?

    fun findPendingProcessByType(type: ProcessType): ProcessDto?

    fun findPendingProcessesByType(type: ProcessType, limit: Int): List<ProcessDto>

    fun hasPendingProcessOfType(type: ProcessType): Boolean

    fun findPendingProcessByTypeAndExternalReference(type: ProcessType, externalReference: String): ProcessDto?

    fun findPendingProcessByTypesAndExternalReference(types: Set<ProcessType>, externalReference: String): ProcessDto?

    fun getProcessTransitions(processId: UUID): List<ProcessTransitionDto>
    fun findProcessesByType(type: ProcessType, pageable: Pageable): Page<ProcessDto>
    fun findProcessByExternalReference(merchantReference: String): ProcessDto?
    fun findRecentProcessesByTypeAndForUserId(processType: ProcessType, userId: UUID, since: Instant): List<ProcessDto>
    fun findActivePendingProcessForTypeAndUser(
        type: ProcessType,
        userId: UUID,
        inactivityThreshold: Instant
    ): ProcessDto?
}