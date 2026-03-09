package com.krachtix.identity.core.totp.query

import an.awesome.pipelinr.Command

class GetTotpStatusQuery : Command<GetTotpStatusResult>

data class GetTotpStatusResult(
    val totpEnabled: Boolean,
    val remainingRecoveryCodes: Long
)
