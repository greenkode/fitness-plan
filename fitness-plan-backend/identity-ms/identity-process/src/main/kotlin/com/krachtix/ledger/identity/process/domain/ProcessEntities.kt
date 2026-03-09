package com.krachtix.identity.process.domain


import com.krachtix.commons.model.AuditableEntity
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.process.domain.model.ProcessEventTransition
import com.krachtix.identity.process.domain.model.ProcessRequest
import com.krachtix.identity.process.domain.model.ProcessRequestData
import com.krachtix.identity.process.domain.model.ProcessStakeholder
import com.krachtix.identity.process.domain.model.SrProcess
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "process")
class ProcessEntity(
    @Column(nullable = false)
    val publicId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private val type: ProcessType,

    @Column(nullable = false)
    private val description: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var state: ProcessState,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private val channel: ProcessChannel,

    @Column(nullable = false)
    val expiry: Instant,

    val externalReference: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToMany(mappedBy = "process", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    val requests: MutableSet<ProcessRequestEntity> = mutableSetOf(),

    @OneToMany(mappedBy = "process", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    val transitions: MutableSet<ProcessEventTransitionEntity> = mutableSetOf(),
) :
    AuditableEntity(), Serializable {
    fun toDomain() = SrProcess(
        publicId,
        type,
        description,
        state,
        id ?: 0L,
        channel,
        createdAt!!,
        requests.map { it.toDomain() }.toSet(),
        transitions.map { it.toDomain() }.toSet(),
        externalReference,
    )

    fun updateState(newState: ProcessState) {
        this.state = newState
    }

    fun addRequest(request: ProcessRequestEntity) = requests.add(request)

    fun updateRequestState(requestId: Long, state: ProcessState) {
        requests.forEach { request ->
            if (request.id == requestId) {
                request.state = state
                return
            }
        }
    }

    fun addTransition(source: ProcessState, target: ProcessState, event: ProcessEvent, initiator: UUID) =
        transitions.add(
            ProcessEventTransitionEntity(
                this,
                event,
                initiator,
                source,
                target
            )
        )
}

@Entity
@Table(name = "process_request")
class ProcessRequestEntity(

    @ManyToOne
    @JoinColumn(name = "process_id", nullable = false)
    private val process: ProcessEntity,

    @Column(nullable = false)
    private val userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private val type: ProcessRequestType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var state: ProcessState,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val channel: ProcessChannel,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToMany(mappedBy = "processRequest", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    private val data: MutableSet<ProcessRequestDataEntity> = mutableSetOf(),

    @OneToMany(mappedBy = "processRequest", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    private val stakeholders: MutableSet<ProcessRequestStakeholder> = mutableSetOf()
) :
    AuditableEntity(), Serializable {
    fun toDomain() = ProcessRequest(
        process.id ?: 0L,
        userId,
        type,
        state,
        channel,
        id ?: 0L,
        data.map { it.toDomain() }.toSet(),
        stakeholders.map { it.toDomain() }.toSet()
    )

    fun addStakeholder(type: ProcessStakeholderType, userId: String) =
        stakeholders.add(ProcessRequestStakeholder(this, userId, type))

    fun addData(name: ProcessRequestDataName, value: String) {
        val item = data.firstOrNull { it.name == name }
        if (item != null) {
            data.remove(item)
        }
        data.add(ProcessRequestDataEntity(this, name, value))
    }

    fun setDataBatch(dataMap: Map<ProcessRequestDataName, String>) {
        // No longer directly manipulating data - handled by batch insert
        // This method kept for compatibility but data insertion is now handled externally
    }
}

@Entity
@Table(name = "process_request_data")
data class ProcessRequestDataEntity(

    @Id
    @ManyToOne
    @JoinColumn(name = "process_request_id", nullable = false)
    private val processRequest: ProcessRequestEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Id val name: ProcessRequestDataName,

    @Column(nullable = false)
    private var value: String,

    ) : AuditableEntity(), Serializable {
    fun toDomain() = ProcessRequestData(processRequest.id ?: 0L, name, value)
}

@Entity
@Table(name = "process_request_stakeholder")
class ProcessRequestStakeholder(

    @ManyToOne
    @JoinColumn(name = "process_request_id", nullable = false)
    private val processRequest: ProcessRequestEntity,

    @Column(nullable = false)
    private val stakeholderId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private val type: ProcessStakeholderType,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

) : AuditableEntity(), Serializable {
    fun toDomain() = ProcessStakeholder(processRequest.id ?: 0L, stakeholderId, type, id ?: 0L)
}

@Entity
@Table(name = "process_event_transition")
class ProcessEventTransitionEntity(

    @ManyToOne
    @JoinColumn(name = "process_id", nullable = false)
    private val process: ProcessEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val event: ProcessEvent,

    @Column(nullable = false)
    val userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val oldState: ProcessState,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val newState: ProcessState,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

) : AuditableEntity(), Serializable {

    fun toDomain() =
        ProcessEventTransition(process.id!!, event, userId, oldState, newState, id ?: 0L)

    fun createdAt() = createdAt!!
}
