package com.krachtix.user.security

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val log = KotlinLogging.logger {}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(
    name = ["security.jwt.jti-validation.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class JtiValidationFilter(
    private val jtiCacheService: JtiCacheService,
    private val jwtDecoder: JwtDecoder
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
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
            val jti = jwt.getClaimAsString("jti")

            if (jti.isNullOrBlank()) {
                log.debug { "No JTI claim in token, skipping replay check" }
                filterChain.doFilter(request, response)
                return
            }

            if (!jtiCacheService.markJtiAsUsed(jti)) {
                log.warn { "Token replay detected for JTI: $jti, request: ${request.method} ${request.requestURI}" }
                sendReplayError(response)
                return
            }

            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            log.debug { "JWT decode failed in JTI filter, deferring to security chain: ${e.message}" }
            filterChain.doFilter(request, response)
        }
    }

    private fun sendReplayError(response: HttpServletResponse) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(
            """{"error":"token_replay","message":"Token has already been used","status":401}"""
        )
    }
}
