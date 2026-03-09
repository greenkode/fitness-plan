package com.krachtix.identity.core.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import com.krachtix.commons.model.AuditableEntity
import java.util.UUID

@Entity
@Table(name = "webhook_config")
class WebhookConfigEntity : AuditableEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable = false, unique = true)
    var publicId: UUID = UUID.randomUUID()

    @Column(nullable = false)
    var clientId: UUID = UUID.randomUUID()

    @Column(nullable = false)
    var name: String = ""

    @Column(nullable = false)
    var url: String = ""

    @Column(nullable = false)
    var secretHash: String = ""

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: WebhookConfigStatus = WebhookConfigStatus.ACTIVE

    @Column(nullable = false)
    var eventTypes: String = ""

    var description: String? = null

    fun getEventTypeList(): List<String> =
        eventTypes.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

    fun setEventTypeList(types: List<String>) {
        eventTypes = types.joinToString(",")
    }
}
