package com.krachtix.identity.core.password.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class CompletePasswordResetCommand(
    val reference: String,
    val token: String,
    val newPassword: CharArray
) : Command<CompletePasswordResetResult> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompletePasswordResetCommand) return false
        return reference == other.reference && token == other.token && newPassword.contentEquals(other.newPassword)
    }

    override fun hashCode(): Int {
        var result = reference.hashCode()
        result = 31 * result + token.hashCode()
        result = 31 * result + newPassword.contentHashCode()
        return result
    }
}

data class CompletePasswordResetResult(
    val success: Boolean,
    val message: String,
    val userId: UUID?
)