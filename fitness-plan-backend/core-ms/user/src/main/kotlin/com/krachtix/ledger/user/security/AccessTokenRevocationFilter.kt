package com.krachtix.user.security

import com.hazelcast.core.HazelcastInstance
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val log = KotlinLogging.logger {}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@ConditionalOnProperty(
    name = ["security.jwt.token-revocation.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class AccessTokenRevocationFilter(
    private val jwtDecoder: JwtDecoder
) : OncePerRequestFilter() {

    @Autowired(required = false)
    private var hazelcastInstance: HazelcastInstance? = null

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val REVOCATION_MAP = "user-token-revocation"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(BEARER_PREFIX.length)

        try {
            val jwt = jwtDecoder.decode(token)
            val userId = jwt.subject
            val issuedAt = jwt.issuedAt?.epochSecond ?: 0

            val revocationMap = hazelcastInstance?.getMap<String, Long>(REVOCATION_MAP)
            val revokedBefore = revocationMap?.get(userId)

            if (revokedBefore != null && issuedAt <= revokedBefore) {
                log.warn { "Revoked access token used for user: $userId, issued at: $issuedAt, revoked before: $revokedBefore" }
                sendRevocationError(response)
                return
            }
        } catch (e: Exception) {
            log.debug { "JWT decode failed in revocation filter, deferring to security chain: ${e.message}" }
        }

        filterChain.doFilter(request, response)
    }

    private fun sendRevocationError(response: HttpServletResponse) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(
            """{"error":"token_revoked","message":"Token has been revoked","status":401}"""
        )
    }
}
