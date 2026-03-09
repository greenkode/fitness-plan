package com.krachtix.identity.core.currency.domain

import com.krachtix.commons.model.AuditableEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "organization_currency")
class OrganizationCurrency(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    val clientId: String,

    val currencyCode: String,

    val isPrimary: Boolean = false,

    val enabled: Boolean = true
) : AuditableEntity()
