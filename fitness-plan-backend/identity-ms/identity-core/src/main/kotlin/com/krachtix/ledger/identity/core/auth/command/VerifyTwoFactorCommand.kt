package com.krachtix.identity.core.auth.command

import an.awesome.pipelinr.Command
import jakarta.servlet.http.HttpServletRequest

data class VerifyTwoFactorCommand(
    val sessionId: String,
    val code: String,
    val deviceName: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val httpRequest: HttpServletRequest? = null
) : Command<VerifyTwoFactorResult>

data class VerifyTwoFactorResult(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val scope: String = "openid email phone profile",
    val user: TwoFactorUserInfo,
    val message: String
)

data class TwoFactorUserInfo(
    val username: String,
    val name: String,
    val email: String
)