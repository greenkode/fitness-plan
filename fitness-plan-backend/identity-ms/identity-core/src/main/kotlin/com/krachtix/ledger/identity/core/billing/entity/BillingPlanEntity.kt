package com.krachtix.identity.core.billing.entity

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
import jakarta.persistence.Table
import com.krachtix.commons.model.AuditableEntity
import com.krachtix.identity.core.organization.entity.Organization
import com.krachtix.identity.core.organization.entity.SubscriptionTier
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "billing_plan")
class BillingPlanEntity(
    @Column(nullable = false)
    val publicId: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var platformFeeAmount: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false)
    var perAccountFeeAmount: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false)
    var perTransactionFeeAmount: BigDecimal = BigDecimal.ZERO,
    var maxChargeAmount: BigDecimal? = null,
    @Column(nullable = false)
    var currency: String = "USD",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var billingCycle: BillingCycle = BillingCycle.MONTHLY,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BillingPlanStatus = BillingPlanStatus.ACTIVE,
    @Column(nullable = false)
    var effectiveFrom: Instant,
    var effectiveUntil: Instant? = null,
    @Enumerated(EnumType.STRING)
    var subscriptionTier: SubscriptionTier? = null,
    var stripePriceId: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null
) : AuditableEntity()
