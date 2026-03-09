package com.krachtix.user.dao

import com.krachtix.commons.model.TenantAwareEntity
import com.krachtix.user.AccessTokenType
import com.krachtix.user.domain.AccessToken
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "access_token")
class AccessTokenEntity(

    @Enumerated(EnumType.STRING)
    val type: AccessTokenType,

    val expiry: Instant,

    val accessToken: String,

    val refreshToken: String?,

    val resource: String,

    val institution: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null
) : TenantAwareEntity() {

    fun toDomain() = AccessToken(type, accessToken, refreshToken, expiry, resource, institution)
}
