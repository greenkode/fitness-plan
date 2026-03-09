package com.krachtix.identity.core.invitation.command

import com.krachtix.commons.dto.PhoneNumber
import an.awesome.pipelinr.Command
import java.util.UUID

data class CompleteInvitationCommand(
    val token: String,
    val reference: String,
    val fullName: String,
    val password: String,
    val phoneNumber: PhoneNumber
) : Command<CompleteInvitationResult>

data class CompleteInvitationResult(
    val success: Boolean,
    val message: String,
    val userId: UUID
)