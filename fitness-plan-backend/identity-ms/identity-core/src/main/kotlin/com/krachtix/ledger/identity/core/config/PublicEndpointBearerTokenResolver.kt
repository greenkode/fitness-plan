package com.krachtix.identity.core.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher

@Component
class PublicEndpointBearerTokenResolver : BearerTokenResolver {

    private val delegate = DefaultBearerTokenResolver()
    private val pathMatcher = AntPathMatcher()

    private val publicPaths = listOf(
        "/api/registration/**",
        "/api/login",
        "/api/2fa/login",
        "/api/2fa/verify",
        "/api/2fa/resend",
        "/api/2fa/refresh",
        "/password-reset/**",
        "/merchant/invitation/validate",
        "/merchant/invitation/complete",
        "/oauth2/**",
        "/.well-known/**",
        "/actuator/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/login/oauth2/code/**",
        "/oauth2/authorization/**",
        "/error",
        "/test/**"
    )

    override fun resolve(request: HttpServletRequest): String? {
        val requestPath = request.requestURI

        if (publicPaths.any { pathMatcher.match(it, requestPath) }) {
            return null
        }

        return delegate.resolve(request)
    }
}
