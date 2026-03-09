package com.krachtix.identity.core.totp.command

import an.awesome.pipelinr.Command

data class ConfirmTotpSetupCommand(
    val code: String
) : Command<ConfirmTotpSetupResult>

data class ConfirmTotpSetupResult(
    val recoveryCodes: List<String>,
    val message: String
)
