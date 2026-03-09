package com.krachtix.user.spi

import com.krachtix.core.commons.client.ClientAuthContext
import com.krachtix.user.integration.identity.CustomJwtClaims.CLIENT_TYPE
import com.krachtix.user.integration.identity.CustomJwtClaims.MERCHANT_ID
import com.krachtix.user.integration.identity.CustomJwtClaims.SCOPE
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ClientService : ClientAuthContext {

    override fun getLoggedInClientId(): UUID? {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
        val jwt = jwtAuthenticationToken?.principal as? Jwt
        return jwt?.getClaimAsString(MERCHANT_ID)?.let { UUID.fromString(it) }
    }

    override fun getClientType(): String? {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
        val jwt = jwtAuthenticationToken?.principal as? Jwt
        return jwt?.getClaimAsString(CLIENT_TYPE)
    }

    override fun getScopes(): Set<String> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
        val jwt = jwtAuthenticationToken?.principal as? Jwt
        val scopeString = jwt?.getClaimAsString(SCOPE) ?: return emptySet()
        return scopeString.split(" ").toSet()
    }

    override fun hasScope(scope: String): Boolean {
        return getScopes().contains(scope)
    }

    override fun isServiceClient(): Boolean {
        return getClientType() == "service"
    }
}
