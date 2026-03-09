package com.krachtix.identity.core.settings.query

import an.awesome.pipelinr.Command
import java.util.UUID

class GetSetupStatusQuery : Command<GetSetupStatusResult>

data class GetSetupStatusResult(
    val processId: UUID?,
    val currentStep: Int,
    val completedSteps: List<String>,
    val isComplete: Boolean,
    val stepData: Map<String, Map<String, String>> = emptyMap()
)
