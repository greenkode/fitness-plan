package com.krachtix.user.spi

import com.krachtix.commons.dto.UserDetailsDto
import com.krachtix.commons.user.UserGateway
import com.krachtix.user.integration.identity.IdentityIntegration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    val identityIntegration: IdentityIntegration,

    ) : UserGateway {

    override fun getLoggedInUserId(): UUID? {

        val token = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken

        val jwt = token?.principal as? Jwt

        return jwt?.let { UUID.fromString(it.id) }
    }

    override fun getSystemUserId(): UUID {

        return UUID.fromString("b34a12c9-7f2b-4955-9da1-47f6fba23892")
    }

    final override fun getLoggedInUserDetails(): UserDetailsDto? {

        val authentication = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken

        val credentials = authentication.credentials as Jwt

        return getUserDetailsById(UUID.fromString(credentials.id))
    }

    override fun findByPublicId(id: UUID): UserDetailsDto? {
        return getUserDetailsById(id)
    }

    override fun getUserDetailsById(id: UUID): UserDetailsDto? {
        return identityIntegration.getKycUserDetails(id)
    }

    override fun getAuthenticatedUserClaims(): Map<String, Any> {

        val authentication = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken

        val credentials = authentication.credentials as Jwt

        return credentials.claims
    }

    override fun authorizeAction(userId: UUID, pin: String): Boolean {

        return true
    }
}