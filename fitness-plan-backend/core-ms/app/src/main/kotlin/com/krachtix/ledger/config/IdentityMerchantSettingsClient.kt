package com.krachtix.config

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.cache.CacheNames
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.merchant.MerchantSettingsDto
import com.krachtix.commons.merchant.MerchantSettingsGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class IdentityMerchantSettingsClient(
    private val restClient: RestClient,
    private val authorizedClientManager: OAuth2AuthorizedClientManager,
    private val messageService: MessageService,
    @Value("\${integration.identity-ms.base-url}") private val identityMsBaseUrl: String,
    @Value("\${integration.identity-ms.oauth2.client-registration-id}") private val clientRegistrationId: String,
    @Value("\${integration.identity-ms.oauth2.principal}") private val principal: String
) : MerchantSettingsGateway {

    @Cacheable(cacheNames = [CacheNames.MERCHANT_SETTINGS], key = "#merchantId.toString()")
    override fun getMerchantSettings(merchantId: UUID): MerchantSettingsDto {
        log.debug { "Fetching merchant settings from identity-ms for merchantId=$merchantId" }

        val authorizeRequest = OAuth2AuthorizeRequest
            .withClientRegistrationId(clientRegistrationId)
            .principal(principal)
            .build()

        val accessToken = authorizedClientManager.authorize(authorizeRequest)?.accessToken?.tokenValue
            ?: throw RecordNotFoundException(
                messageService.getMessage("merchant.error.settings.access_token_failed")
            )

        val response = restClient.get()
            .uri("$identityMsBaseUrl/api/internal/merchants/$merchantId/settings")
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .body(MerchantSettingsResponse::class.java)
            ?: throw RecordNotFoundException(
                messageService.getMessage("merchant.error.settings.not_found", merchantId)
            )

        val defaultCurrency = response.defaultCurrency
            ?.takeIf { it.isNotBlank() }
            ?: throw RecordNotFoundException(
                messageService.getMessage("merchant.error.settings.no_currencies_configured", merchantId)
            )

        val chartTemplateId = response.chartTemplateId
            ?.takeIf { it.isNotBlank() }
            ?: throw RecordNotFoundException(
                messageService.getMessage("merchant.error.settings.chart_template_not_configured", merchantId)
            )

        return MerchantSettingsDto(
            defaultCurrency = defaultCurrency,
            additionalCurrencies = response.additionalCurrencies,
            chartTemplateId = UUID.fromString(chartTemplateId),
            setupCompleted = response.setupCompleted,
            subscriptionTier = response.subscriptionTier ?: "TRIAL",
            maxCurrencies = response.maxCurrencies ?: 1,
            timezone = response.timezone,
            organizationName = response.organizationName ?: "",
            restricted = response.restricted
        )
    }
}

private data class MerchantSettingsResponse(
    val defaultCurrency: String? = null,
    val additionalCurrencies: List<String> = emptyList(),
    val chartTemplateId: String? = null,
    val setupCompleted: Boolean = false,
    val subscriptionTier: String? = null,
    val maxCurrencies: Int? = null,
    val timezone: String? = null,
    val organizationName: String? = null,
    val restricted: Boolean = false
)
