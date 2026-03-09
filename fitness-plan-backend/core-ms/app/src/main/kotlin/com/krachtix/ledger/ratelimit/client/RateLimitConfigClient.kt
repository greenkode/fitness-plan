package com.krachtix.ratelimit.client

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RateLimitConfigDto(
    val methodName: String,
    val subscriptionTier: String? = null,
    val scope: String? = null,
    val capacity: Int = 100,
    val timeValue: Int = 1,
    val timeUnit: ChronoUnit = ChronoUnit.MINUTES
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RateLimitConfigsResponse(
    val configs: List<RateLimitConfigDto>
)

@Component
class RateLimitConfigClient(
    private val restClient: RestClient,
    private val authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${integration.identity-ms.base-url}") private val identityMsBaseUrl: String,
    @Value("\${integration.identity-ms.oauth2.client-registration-id}") private val clientRegistrationId: String,
    @Value("\${integration.identity-ms.oauth2.principal}") private val principal: String
) {

    fun fetchAllConfigs(): List<RateLimitConfigDto> {
        log.debug { "Fetching rate limit configs from identity-ms" }

        val authorizeRequest = OAuth2AuthorizeRequest
            .withClientRegistrationId(clientRegistrationId)
            .principal(principal)
            .build()

        val authorizedClient = authorizedClientManager.authorize(authorizeRequest)
        val accessToken = authorizedClient?.accessToken?.tokenValue
            ?: run {
                log.warn { "Failed to obtain access token for identity-ms, using default rate limits" }
                return emptyList()
            }

        return runCatching {
            restClient.get()
                .uri("$identityMsBaseUrl/api/internal/rate-limits")
                .headers { it.setBearerAuth(accessToken) }
                .retrieve()
                .body(RateLimitConfigsResponse::class.java)
                ?.configs ?: emptyList()
        }.getOrElse { ex ->
            log.error(ex) { "Failed to fetch rate limit configs from identity-ms" }
            emptyList()
        }
    }
}
