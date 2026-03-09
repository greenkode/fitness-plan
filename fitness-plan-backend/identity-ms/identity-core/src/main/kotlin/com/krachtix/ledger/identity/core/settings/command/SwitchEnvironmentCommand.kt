package com.krachtix.identity.core.settings.command

import com.krachtix.identity.core.entity.EnvironmentMode
import com.krachtix.identity.core.settings.dto.EnvironmentStatusResponse
import an.awesome.pipelinr.Command

data class SwitchEnvironmentCommand(
    val environment: EnvironmentMode
) : Command<EnvironmentStatusResponse>
