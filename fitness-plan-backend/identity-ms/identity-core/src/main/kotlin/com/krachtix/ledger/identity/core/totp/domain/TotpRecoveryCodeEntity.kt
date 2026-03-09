package com.krachtix.identity.core.totp.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import com.krachtix.commons.model.AuditableEntity
import com.krachtix.identity.core.entity.OAuthUser
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "totp_recovery_code")
class TotpRecoveryCodeEntity(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: OAuthUser,

    @Column(nullable = false)
    val codeHash: String = "",

    var used: Boolean = false,

    var usedAt: Instant? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null
) : AuditableEntity()
