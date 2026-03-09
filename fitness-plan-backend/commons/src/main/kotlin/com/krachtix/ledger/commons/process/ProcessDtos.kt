package com.krachtix.commons.process


import io.github.oshai.kotlinlogging.KotlinLogging.logger
import java.io.Serializable
import com.krachtix.commons.exception.IllegalProcessDataException
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.commons.util.Constants.Companion.MISSING_PROCESS_DATA
import java.time.Instant
import java.util.UUID

data class ProcessDto(
    val id: Long,
    val publicId: UUID,
    val state: ProcessState,
    val type: ProcessType,
    val channel: ProcessChannel,
    val createdDate: Instant,
    val requests: List<ProcessRequestDto>,
    val externalReference: String? = null
) : Serializable {

    fun open(): Boolean = state == ProcessState.PENDING

    fun getInitialRequest(): ProcessRequestDto {
        return requests.first { it.type == ProcessRequestType.CREATE_NEW_PROCESS }
    }

    fun getRequestByType(requestType: ProcessRequestType): ProcessRequestDto {
        return requests.first { it.type == requestType }
    }

    fun getLatestRequestByType(requestType: ProcessRequestType): ProcessRequestDto {
        return requests.sortedBy { it -> it.id }.last { it.type == requestType }
    }

    fun findDataValueInRequests(dataName: ProcessRequestDataName): String? =
        requests.firstOrNull { it.data.containsKey(dataName) }?.getDataValueOrNull(dataName)
}

data class ProcessRequestDto(
    val id: Long,
    val type: ProcessRequestType,
    val state: ProcessState,
    val stakeholders: Map<ProcessStakeholderType, String>,
    val data: Map<ProcessRequestDataName, String>
) : Serializable {

    private val log = logger {}

    fun getDataValue(dataName: ProcessRequestDataName): String = data[dataName] ?: run {
        log.error { "No data found for $dataName, request id: $id" }
        throw IllegalProcessDataException(
            MISSING_PROCESS_DATA
        )
    }

    fun getDataValueOrNull(dataName: ProcessRequestDataName): String? = data[dataName]

    fun getDataValueOrEmpty(dataName: ProcessRequestDataName): String = data[dataName].orEmpty()

    fun getStakeholderValue(stakeholderType: ProcessStakeholderType): String =
        stakeholders[stakeholderType] ?: throw IllegalProcessDataException(
            MISSING_PROCESS_DATA
        )
}

data class CreateNewProcessPayload(
    val userId: UUID,
    val publicId: UUID,
    val type: ProcessType,
    val description: String,
    val initialState: ProcessState,
    val requestState: ProcessState,
    val channel: ProcessChannel,
    val data: Map<ProcessRequestDataName, String> = mapOf(),
    val stakeholders: Map<ProcessStakeholderType, String> = mapOf(),
    val externalReference: String? = null,
)

data class MakeProcessRequestPayload(
    val userId: UUID,
    val publicId: UUID,
    val eventType: ProcessEvent,
    val requestType: ProcessRequestType,
    val channel: ProcessChannel,
    val state: ProcessState = ProcessState.PENDING,
    val data: Map<ProcessRequestDataName, String> = mapOf(),
    val stakeholders: Map<ProcessStakeholderType, String> = mapOf(),
)

data class ProcessTransitionDto(
    val id: Long,
    val event: ProcessEvent,
    val userId: UUID,
    val oldState: ProcessState,
    val newState: ProcessState,
    val timestamp: Instant
) : Serializable