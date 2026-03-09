package com.krachtix.user.spi

import com.krachtix.core.commons.client.ClientAuthContext
import com.krachtix.commons.client.ClientDetailsDto
import com.krachtix.commons.client.ClientGateway
import com.krachtix.user.integration.identity.ClientIntegration
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ClientDetailsService(
    private val clientAuthContext: ClientAuthContext,
    private val clientIntegration: ClientIntegration
) : ClientGateway {

    companion object {
        const val CLIENT_DETAILS_CACHE = "krachtix-clientDetails"
    }

    @Cacheable(cacheNames = [CLIENT_DETAILS_CACHE], key = "#root.target.clientAuthContext.getLoggedInClientId().toString()",
        condition = "#root.target.clientAuthContext.getLoggedInClientId() != null", unless = "#result == null")
    override fun getLoggedInClientDetails(): ClientDetailsDto? {
        val clientId = clientAuthContext.getLoggedInClientId() ?: return null
        return getClientDetailsByIdInternal(clientId)
    }

    @Cacheable(cacheNames = [CLIENT_DETAILS_CACHE], key = "#clientId.toString()", unless = "#result == null")
    override fun getClientDetailsById(clientId: UUID): ClientDetailsDto? {
        return getClientDetailsByIdInternal(clientId)
    }

    private fun getClientDetailsByIdInternal(clientId: UUID): ClientDetailsDto? {
        return clientIntegration.getClientDetails(clientId)
    }
}
