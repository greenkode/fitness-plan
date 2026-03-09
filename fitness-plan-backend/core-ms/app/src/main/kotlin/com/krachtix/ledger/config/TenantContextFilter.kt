package com.krachtix.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import com.krachtix.commons.tenant.TenantContext
import com.krachtix.commons.tenant.TenantContextData
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Order(0)
class TenantContextFilter : OncePerRequestFilter() {

    companion object {
        const val MERCHANT_ID_CLAIM = "merchant_id"

        private val EXCLUDED_PATHS = setOf(
            "/actuator",
            "/health",
            "/swagger",
            "/v3/api-docs",
            "/platform"
        )
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return EXCLUDED_PATHS.any { path.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            setupTenantContext()
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }

    private fun setupTenantContext() {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication !is JwtAuthenticationToken) {
            log.debug { "No JWT authentication found, skipping tenant context setup" }
            return
        }

        val jwt = authentication.token
        val merchantIdStr = jwt.getClaimAsString(MERCHANT_ID_CLAIM)

        if (merchantIdStr.isNullOrBlank()) {
            log.debug { "No merchant_id claim in JWT, skipping tenant context setup" }
            return
        }

        val merchantId = runCatching { UUID.fromString(merchantIdStr) }.getOrNull()
            ?: run {
                log.warn { "Invalid merchant_id format: $merchantIdStr" }
                return
            }

        val contextData = TenantContextData(merchantId = merchantId)
        TenantContext.setContext(contextData)
        log.debug { "Tenant context set: merchant=$merchantId" }
    }
}
