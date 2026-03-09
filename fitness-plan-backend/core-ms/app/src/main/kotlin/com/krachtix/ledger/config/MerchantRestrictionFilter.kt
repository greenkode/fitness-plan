package com.krachtix.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import com.krachtix.commons.billing.MerchantRestrictionGateway
import com.krachtix.commons.tenant.TenantContext
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val log = KotlinLogging.logger {}

@Component
@Order(2)
class MerchantRestrictionFilter(
    private val merchantRestrictionGateway: MerchantRestrictionGateway
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val method = request.method
        if (method in READ_ONLY_METHODS) {
            chain.doFilter(request, response)
            return
        }

        val merchantId = TenantContext.getMerchantIdOrNull()
        if (merchantId != null && merchantRestrictionGateway.isMerchantRestricted(merchantId)) {
            log.warn { "Blocked $method ${request.requestURI} for restricted merchant $merchantId" }
            response.status = 402
            response.contentType = "application/json"
            response.writer.write(
                """{"error":"payment_required","message":"Your account is restricted due to an unpaid invoice. Please complete payment to restore full access."}"""
            )
            return
        }

        chain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.startsWith("/api/webhooks/") ||
            request.requestURI.startsWith("/internal/") ||
            request.requestURI.startsWith("/actuator/") ||
            request.requestURI.startsWith("/webhooks/") ||
            request.requestURI.startsWith("/swagger") ||
            request.requestURI.startsWith("/v3/api-docs")

    companion object {
        private val READ_ONLY_METHODS = setOf("GET", "HEAD", "OPTIONS")
    }
}
