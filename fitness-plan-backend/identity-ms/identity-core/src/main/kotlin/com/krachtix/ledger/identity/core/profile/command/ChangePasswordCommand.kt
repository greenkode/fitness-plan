package com.krachtix.identity.core.profile.command

import an.awesome.pipelinr.Command

data class ChangePasswordCommand(
    val currentPassword: CharArray,
    val newPassword: CharArray
) : Command<ChangePasswordResult> {

    override fun equals(other: kotlin.Any?): Boolean {
        if (this === other) return true
        if (other !is ChangePasswordCommand) return false
        return currentPassword.contentEquals(other.currentPassword) && newPassword.contentEquals(other.newPassword)
    }

    override fun hashCode(): Int {
        var result = currentPassword.contentHashCode()
        result = 31 * result + newPassword.contentHashCode()
        return result
    }
}

data class ChangePasswordResult(
    val success: Boolean,
    val message: String
)
