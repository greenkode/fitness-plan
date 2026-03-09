package com.krachtix.identity.core.ratelimit.domain

import com.krachtix.commons.model.AuditableEntity
import com.krachtix.identity.core.organization.entity.SubscriptionTier
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
class RateLimitConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val methodName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val subscriptionTier: SubscriptionTier,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val scope: RateLimitScope = RateLimitScope.INDIVIDUAL,

    @Column(nullable = false)
    val capacity: Int,

    @Column(nullable = false)
    val timeValue: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val timeUnit: ChronoUnit,

    val active: Boolean = true,

) : AuditableEntity()
