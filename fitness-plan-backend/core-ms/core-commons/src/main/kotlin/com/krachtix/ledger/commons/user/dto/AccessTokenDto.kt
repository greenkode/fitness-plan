package com.krachtix.user.dto

import com.krachtix.user.AccessTokenType
import java.time.Instant

data class AccessTokenDto(
    val type: AccessTokenType, val accessToken: String, val refreshToken: String?,
    val expiry: Instant, val resource: String, val institution: String
)