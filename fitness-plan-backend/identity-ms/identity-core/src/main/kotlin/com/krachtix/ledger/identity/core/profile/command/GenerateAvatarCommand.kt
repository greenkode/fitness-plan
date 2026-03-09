package com.krachtix.identity.core.profile.command

import com.krachtix.identity.core.profile.dto.AvatarStyle
import an.awesome.pipelinr.Command
import java.util.UUID

data class GenerateAvatarCommand(
    val style: AvatarStyle,
    val backgroundColor: String?,
    val prompt: String? = null,
    val processId: UUID? = null
) : Command<GenerateAvatarResult>

data class GenerateAvatarResult(
    val success: Boolean,
    val message: String,
    val pictureUrl: String?,
    val processId: UUID? = null,
    val imageKey: String? = null
)
