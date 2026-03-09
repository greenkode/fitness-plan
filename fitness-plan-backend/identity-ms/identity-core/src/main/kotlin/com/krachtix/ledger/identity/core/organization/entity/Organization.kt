package com.krachtix.identity.core.organization.entity

import com.krachtix.commons.model.AuditableEntity
import com.krachtix.identity.core.entity.OrganizationStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class SubscriptionTier {
    TRIAL,
    STARTER,
    PROFESSIONAL,
    ENTERPRISE
}

@Entity
@Table(name = "organization")
class Organization(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var slug: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrganizationStatus = OrganizationStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var plan: SubscriptionTier = SubscriptionTier.TRIAL,

    var databaseName: String? = null,

    @Column(nullable = false)
    var databaseCreated: Boolean = false,

    @Column(nullable = false)
    var maxKnowledgeBases: Int = 5,

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),

    var stripeCustomerId: String? = null,

    var stripeSubscriptionId: String? = null,

    @Column(nullable = false)
    var restricted: Boolean = false,

    var restrictedAt: Instant? = null,

    var restrictionReason: String? = null
) : AuditableEntity() {

    @OneToMany(mappedBy = "organization", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val properties: MutableSet<OrganizationProperty> = mutableSetOf()

    constructor() : this(
        id = UUID.randomUUID(),
        name = "",
        slug = ""
    )

    fun addProperty(name: OrganizationPropertyName, value: String) {
        properties.removeIf { it.propertyName == name }
        properties.add(OrganizationProperty(this, name, value))
    }

    fun getProperty(name: OrganizationPropertyName): String? =
        properties.find { it.propertyName == name }?.propertyValue

    fun generateDatabaseName(): String = "org_$slug"
}
