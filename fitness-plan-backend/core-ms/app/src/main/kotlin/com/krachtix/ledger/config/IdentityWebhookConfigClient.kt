package com.krachtix.config

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.cache.CacheNames
import com.krachtix.commons.webhook.WebhookConfigDto
import com.krachtix.commons.webhook.WebhookConfigGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class IdentityWebhookConfigClient(
    private val restClient: RestClient,
    private val authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${integration.identity-ms.base-url}") private val identityMsBaseUrl: String,
    @Value("\${integration.identity-ms.oauth2.client-registration-id}") private val clientRegistrationId: String,
    @Value("\${integration.identity-ms.oauth2.principal}") private val principal: String
) : WebhookConfigGateway {

    @Cacheable(cacheNames = [CacheNames.WEBHOOK_CONFIG], key = "#merchantId.toString() + ':' + #eventType", unless = "#result == null || #result.isEmpty()")
    override fun getActiveWebhooks(merchantId: UUID, eventType: String): List<WebhookConfigDto> {
        log.debug { "Fetching active webhooks from identity-ms for merchantId=$merchantId, eventType=$eventType" }

        val accessToken = getAccessToken()

        return runCatching {
            restClient.get()
                .uri("$identityMsBaseUrl/api/internal/webhooks/active?merchantId=$merchantId&eventType=$eventType")
                .headers { it.setBearerAuth(accessToken) }
                .retrieve()
                .body(object : ParameterizedTypeReference<List<InternalActiveWebhookResponse>>() {})
                ?.map { it.toDto() }
                ?: emptyList()
        }.getOrElse { ex ->
            log.error(ex) { "Failed to fetch active webhooks from identity-ms for merchant=$merchantId" }
            emptyList()
        }
    }

    override fun getWebhookByPublicId(publicId: UUID): WebhookConfigDto? {
        log.debug { "Fetching webhook config from identity-ms for publicId=$publicId" }

        val accessToken = getAccessToken()

        return runCatching {
            restClient.get()
                .uri("$identityMsBaseUrl/api/internal/webhooks/$publicId")
                .headers { it.setBearerAuth(accessToken) }
                .retrieve()
                .body(InternalActiveWebhookResponse::class.java)
                ?.toDto()
        }.getOrElse { ex ->
            log.error(ex) { "Failed to fetch webhook config $publicId from identity-ms" }
            null
        }
    }

    private fun getAccessToken(): String {
        val authorizeRequest = OAuth2AuthorizeRequest
            .withClientRegistrationId(clientRegistrationId)
            .principal(principal)
            .build()

        return authorizedClientManager.authorize(authorizeRequest)?.accessToken?.tokenValue
            ?: throw IllegalStateException("Failed to obtain access token for identity-ms webhook API")
    }
}

private data class InternalActiveWebhookResponse(
    val publicId: UUID = UUID.randomUUID(),
    val url: String = "",
    val signingSecret: String = "",
    val eventTypes: List<String> = emptyList()
) {
    fun toDto() = WebhookConfigDto(
        publicId = publicId,
        url = url,
        signingSecret = signingSecret,
        eventTypes = eventTypes
    )
}
