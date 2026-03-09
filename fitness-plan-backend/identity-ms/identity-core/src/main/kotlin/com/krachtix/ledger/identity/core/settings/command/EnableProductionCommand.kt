package com.krachtix.identity.core.settings.command

import com.krachtix.identity.core.entity.EnvironmentMode
import an.awesome.pipelinr.Command

data class EnableProductionCommand(
    val environmentMode: EnvironmentMode
) : Command<EnableProductionResult>

data class EnableProductionResult(
    val merchantId: String,
    val environmentMode: EnvironmentMode,
    val affectedUsers: Int,
    val success: Boolean,
    val message: String
)
