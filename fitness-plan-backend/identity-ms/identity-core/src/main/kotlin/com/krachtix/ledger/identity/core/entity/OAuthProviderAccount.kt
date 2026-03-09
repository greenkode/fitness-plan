package com.krachtix.identity.core.entity

import com.krachtix.commons.model.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Table
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "oauth_provider_account")
class OAuthProviderAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: OAuthUser,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val provider: OAuthProvider,

    @Column(nullable = false)
    val providerUserId: String,

    val providerEmail: String? = null,

    @Column(nullable = false)
    val linkedAt: Instant = Instant.now(),

    var lastLoginAt: Instant? = null
) : AuditableEntity() {

    fun updateLastLogin() {
        this.lastLoginAt = Instant.now()
    }
}
