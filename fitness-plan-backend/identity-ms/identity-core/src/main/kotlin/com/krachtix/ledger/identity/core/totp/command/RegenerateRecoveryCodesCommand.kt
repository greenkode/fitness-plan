package com.krachtix.identity.core.totp.command

import an.awesome.pipelinr.Command

data class RegenerateRecoveryCodesCommand(
    val code: String
) : Command<RegenerateRecoveryCodesResult>

data class RegenerateRecoveryCodesResult(
    val recoveryCodes: List<String>,
    val message: String
)
