package com.krachtix.config

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.billing.MerchantRestrictionGateway
import com.krachtix.commons.cache.CacheNames
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class IdentityRestrictionClient(
    private val restClient: RestClient,
    private val authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${integration.identity-ms.base-url}") private val identityMsBaseUrl: String,
    @Value("\${integration.identity-ms.oauth2.client-registration-id}") private val clientRegistrationId: String,
    @Value("\${integration.identity-ms.oauth2.principal}") private val principal: String
) : MerchantRestrictionGateway {

    @Cacheable(cacheNames = [CacheNames.MERCHANT_RESTRICTION], key = "#merchantId.toString()", unless = "#result == null")
    override fun isMerchantRestricted(merchantId: UUID): Boolean {
        log.debug { "Checking restriction status for merchant=$merchantId" }

        val accessToken = getAccessToken()

        val response = restClient.get()
            .uri("$identityMsBaseUrl/internal/organizations/$merchantId/restriction")
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .body(RestrictionResponse::class.java)

        return response?.restricted ?: false
    }

    @CacheEvict(cacheNames = [CacheNames.MERCHANT_RESTRICTION], key = "#merchantId.toString()")
    override fun restrictMerchant(merchantId: UUID, reason: String) {
        log.info { "Restricting merchant=$merchantId, reason=$reason" }

        val accessToken = getAccessToken()

        restClient.put()
            .uri("$identityMsBaseUrl/internal/organizations/$merchantId/restriction")
            .headers { it.setBearerAuth(accessToken) }
            .body(mapOf("reason" to reason))
            .retrieve()
            .toBodilessEntity()

        log.info { "Merchant $merchantId restricted" }
    }

    @CacheEvict(cacheNames = [CacheNames.MERCHANT_RESTRICTION], key = "#merchantId.toString()")
    override fun liftRestriction(merchantId: UUID) {
        log.info { "Lifting restriction for merchant=$merchantId" }

        val accessToken = getAccessToken()

        restClient.delete()
            .uri("$identityMsBaseUrl/internal/organizations/$merchantId/restriction")
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .toBodilessEntity()

        log.info { "Restriction lifted for merchant $merchantId" }
    }

    private fun getAccessToken(): String {
        val authorizeRequest = OAuth2AuthorizeRequest
            .withClientRegistrationId(clientRegistrationId)
            .principal(principal)
            .build()

        return authorizedClientManager.authorize(authorizeRequest)?.accessToken?.tokenValue
            ?: throw IllegalStateException("Failed to obtain access token for identity-ms restriction API")
    }
}

private data class RestrictionResponse(
    val restricted: Boolean = false,
    val reason: String? = null
)
