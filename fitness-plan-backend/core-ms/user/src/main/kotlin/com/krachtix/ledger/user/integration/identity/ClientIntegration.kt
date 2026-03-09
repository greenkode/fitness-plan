package com.krachtix.user.integration.identity

import com.krachtix.commons.client.ClientDetailsDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class ClientIntegration {

    fun getClientDetails(clientId: UUID): ClientDetailsDto? {
        log.warn { "Client details lookup not supported - returning null for: $clientId" }
        return null
    }
}

data class ClientDetailsResponse(
    val id: UUID,
    val clientId: String,
    val clientName: String,
    val organizationId: UUID?,
    val scopes: Set<String>,
    val environmentMode: String,
    val status: String
) {
    fun toDto() = ClientDetailsDto(
        id = id,
        clientId = clientId,
        clientName = clientName,
        organizationId = organizationId,
        scopes = scopes,
        environmentMode = environmentMode,
        status = status
    )
}
