package com.krachtix.user.dto

import java.util.UUID

data class UpdateUserPayload(
    val userId: UUID,
    val externalIds: Set<UserExternalId> = emptySet(),
    val properties: Set<UserProperty> = emptySet()
)