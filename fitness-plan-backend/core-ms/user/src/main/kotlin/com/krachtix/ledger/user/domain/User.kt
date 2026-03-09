package com.krachtix.user.domain

import com.krachtix.user.dto.UserExternalId
import com.krachtix.user.dto.UserProperty
import java.util.UUID

data class User(
    val userId: UUID,
    val externalIds: MutableSet<UserExternalId> = mutableSetOf(),
    val properties: MutableSet<UserProperty> = mutableSetOf()
)