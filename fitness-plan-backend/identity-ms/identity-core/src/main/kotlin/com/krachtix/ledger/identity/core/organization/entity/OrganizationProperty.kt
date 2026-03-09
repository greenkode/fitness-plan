package com.krachtix.identity.core.organization.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "organization_property")
class OrganizationProperty(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val propertyName: OrganizationPropertyName,

    @Column(nullable = false)
    var propertyValue: String
) : Serializable
