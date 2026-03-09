package com.krachtix.identity.core.totp.command

import an.awesome.pipelinr.Command

class InitiateTotpSetupCommand : Command<InitiateTotpSetupResult>

data class InitiateTotpSetupResult(
    val secret: String,
    val qrCodeUri: String,
    val message: String
)
