package com.krachtix.user.security

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class SigningSecretKeyResolver(
    private val jwtDecoder: JwtDecoder,
    private val signingKeyRepository: SigningKeyRepository
) {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    fun resolveSecretKey(request: HttpServletRequest): String? {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            val token = authHeader.substring(BEARER_PREFIX.length)
            return runCatching {
                val jwt = jwtDecoder.decode(token)
                val merchantId = jwt.getClaimAsString("merchant_id")
                merchantId?.let { signingKeyRepository.findSecretKeyByMerchantId(it) }
            }.onFailure { ex ->
                log.error(ex) { "Failed to resolve signing key from JWT" }
            }.getOrNull()
        }

        return null
    }
}

interface SigningKeyRepository {
    fun findSecretKeyByMerchantId(merchantId: String): String?
}

@Component
class DefaultSigningKeyRepository : SigningKeyRepository {
    override fun findSecretKeyByMerchantId(merchantId: String): String? = null
}
