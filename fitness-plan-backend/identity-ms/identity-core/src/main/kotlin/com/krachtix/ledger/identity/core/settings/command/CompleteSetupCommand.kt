package com.krachtix.identity.core.settings.command

import an.awesome.pipelinr.Command

class CompleteSetupCommand : Command<CompleteSetupResult>

data class CompleteSetupResult(
    val success: Boolean,
    val message: String,
    val merchantId: String? = null
)
