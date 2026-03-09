package com.krachtix.identity.core.totp.command

import an.awesome.pipelinr.Command

data class DisableTotpCommand(
    val code: String? = null,
    val password: String? = null
) : Command<DisableTotpResult>

data class DisableTotpResult(
    val message: String
)
