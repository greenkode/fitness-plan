package com.krachtix.identity.core.refreshtoken.domain

import com.krachtix.commons.model.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "refresh_token",
    indexes = [
        Index(name = "idx_refresh_token_jti", columnList = "jti"),
        Index(name = "idx_refresh_token_user_id", columnList = "user_id")
    ]
)
class RefreshTokenEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val jti: String,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val tokenHash: String,

    val ipAddress: String? = null,

    val userAgent: String? = null,

    val deviceFingerprint: String? = null,

    @Column(nullable = false)
    val issuedAt: Instant = Instant.now(),

    @Column(nullable = false)
    val expiresAt: Instant,

    var revokedAt: Instant? = null,

    var replacedByJti: String? = null

) : AuditableEntity() {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun isRevoked(): Boolean = revokedAt != null

    fun isValid(): Boolean = !isExpired() && !isRevoked()

    fun revoke(replacedBy: String? = null) {
        this.revokedAt = Instant.now()
        this.replacedByJti = replacedBy
    }
}
