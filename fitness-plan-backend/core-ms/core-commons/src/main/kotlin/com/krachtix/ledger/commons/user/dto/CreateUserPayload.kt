package com.krachtix.user.dto

import java.util.UUID

data class CreateUserPayload(
    val userId: UUID,
    val externalIds: MutableSet<UserExternalId> = mutableSetOf(),
    val properties: MutableSet<UserProperty> = mutableSetOf()
)