package com.krachtix.identity.core.invitation.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class RevokeInvitationCommand(
    val targetUserId: UUID,
    val revokedByUserId: UUID
) : Command<RevokeInvitationResult>

data class RevokeInvitationResult(
    val success: Boolean,
    val message: String
)
