package com.krachtix.commons.client

import java.util.UUID

data class ClientDetailsDto(
    val id: UUID,
    val clientId: String,
    val clientName: String,
    val organizationId: UUID?,
    val scopes: Set<String>,
    val environmentMode: String,
    val status: String
)
