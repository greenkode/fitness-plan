package com.krachtix.user.domain

import com.krachtix.user.AccessTokenType
import com.krachtix.user.dto.AccessTokenDto
import java.time.Instant

data class AccessToken(
    val type: AccessTokenType,
    val accessToken: String,
    val refreshToken: String?,
    val expiry: Instant,
    val resource: String,
    val institution: String
) {
    fun toDto() = AccessTokenDto(type, accessToken, refreshToken, expiry, resource, institution)
}