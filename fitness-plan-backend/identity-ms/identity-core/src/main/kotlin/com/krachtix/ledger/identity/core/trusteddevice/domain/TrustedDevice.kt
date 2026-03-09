package com.krachtix.identity.core.trusteddevice.domain

import com.krachtix.commons.model.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant
import java.util.UUID

@Entity
class TrustedDevice(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val deviceFingerprint: String,

    @Column(nullable = false)
    val deviceFingerprintHash: String,

    val deviceName: String? = null,

    val ipAddress: String? = null,

    val userAgent: String? = null,

    @Column(nullable = false)
    val trustedAt: Instant = Instant.now(),

    @Column(nullable = false)
    var expiresAt: Instant,

    @Column(nullable = false)
    var lastUsedAt: Instant = Instant.now(),

    var trustCount: Int = 1

) : AuditableEntity() {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun updateLastUsed() {
        this.lastUsedAt = Instant.now()
    }

    fun extendExpiration(duration: java.time.Duration) {
        this.expiresAt = Instant.now().plus(duration)
        this.trustCount++
    }
}
