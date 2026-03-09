package com.krachtix.identity.core.auth.command

import an.awesome.pipelinr.Command

data class LoginCommand(
    val username: String,
    val password: CharArray,
    val ipAddress: String? = null
) : Command<LoginResult> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoginCommand) return false
        return username == other.username && password.contentEquals(other.password) && ipAddress == other.ipAddress
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + (ipAddress?.hashCode() ?: 0)
        return result
    }
}

data class LoginResult(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer",
    val username: String,
    val fullName: String,
    val email: String
)
