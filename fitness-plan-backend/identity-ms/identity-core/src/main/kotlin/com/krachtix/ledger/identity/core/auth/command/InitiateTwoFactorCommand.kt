package com.krachtix.identity.core.auth.command

import com.krachtix.identity.core.auth.dto.DirectLoginResult
import an.awesome.pipelinr.Command
import jakarta.servlet.http.HttpServletRequest

data class InitiateTwoFactorCommand(
    val username: String,
    val password: CharArray,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val deviceFingerprint: String? = null,
    val httpRequest: HttpServletRequest? = null
) : Command<DirectLoginResult> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InitiateTwoFactorCommand) return false
        return username == other.username && password.contentEquals(other.password) && ipAddress == other.ipAddress
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + (ipAddress?.hashCode() ?: 0)
        return result
    }
}