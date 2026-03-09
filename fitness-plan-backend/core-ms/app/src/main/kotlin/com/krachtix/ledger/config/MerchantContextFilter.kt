package com.krachtix.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import com.krachtix.commons.merchant.MerchantContext
import com.krachtix.commons.merchant.MerchantContextData
import com.krachtix.commons.merchant.MerchantSettingsGateway
import com.krachtix.commons.tenant.TenantContext
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val log = KotlinLogging.logger {}

@Component
@Order(1)
class MerchantContextFilter(
    private val merchantSettingsGateway: MerchantSettingsGateway
) : OncePerRequestFilter() {

    companion object {
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
            setupMerchantContext()
            filterChain.doFilter(request, response)
        } finally {
            MerchantContext.clear()
        }
    }

    private fun setupMerchantContext() {
        if (!TenantContext.isSet()) {
            log.debug { "No tenant context set, skipping merchant context setup" }
            return
        }

        val merchantId = TenantContext.getMerchantId()
        try {
            val settings = merchantSettingsGateway.getMerchantSettings(merchantId)
            MerchantContext.setContext(MerchantContextData(settings))
            log.debug { "Merchant context set: merchant=$merchantId, timezone=${settings.timezone}" }
        } catch (e: Exception) {
            log.warn(e) { "Failed to load merchant settings for merchant=$merchantId" }
        }
    }
}
