package com.krachtix.user.dto

import java.io.Serializable

data class UserExternalId(
    val externalId: String,
    val integratorCode: String,
    val integrator: String
) : Serializable