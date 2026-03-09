package com.krachtix.identity.process.domain.model

import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.ProcessRequestDto
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import java.time.Instant
import java.util.UUID

data class SrProcess(
    val publicId: UUID,
    val type: ProcessType,
    val description: String,
    val state: ProcessState,
    val id: Long,
    val channel: ProcessChannel,
    val createdAt: Instant,
    val requests: Set<ProcessRequest>,
    val transitions: Set<ProcessEventTransition>,
    val externalReference: String? = null,
    val integratorReference: String? = null,
) {

    fun toDto(): ProcessDto {
        return ProcessDto(id, publicId, state, type, channel, createdAt, requests.map { request ->
            ProcessRequestDto(
                request.id,
                request.type,
                request.state,
                request.stakeholders(),
                request.toMap()
            )
        }, externalReference)
    }
}


data class ProcessRequest(
    val processId: Long,
    val stakeholderId: UUID,
    val type: ProcessRequestType,
    val state: ProcessState,
    val channel: ProcessChannel,
    val id: Long,
    val data: Set<ProcessRequestData>,
    val stakeholders: Set<ProcessStakeholder>
) {
    fun getData(name: ProcessRequestDataName): ProcessRequestData? {

        return data.firstOrNull { it.name == name }
    }

    fun toMap() = data.associate { data -> data.name to data.value }

    fun stakeholders() = stakeholders.associate { it.type to it.stakeholderId }
}


data class ProcessRequestData(
    val processRequestId: Long,
    val name: ProcessRequestDataName,
    val value: String
)


data class ProcessStakeholder(
    val processRequestId: Long,
    val stakeholderId: String,
    val type: ProcessStakeholderType,
    val id: Long
)

data class ProcessEventTransition(
    val processId: Long,
    val event: ProcessEvent,
    val userId: UUID,
    val oldState: ProcessState,
    val newState: ProcessState,
    val id: Long
)