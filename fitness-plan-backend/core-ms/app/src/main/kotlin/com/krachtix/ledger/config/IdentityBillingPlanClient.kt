package com.krachtix.config

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.billing.BillingPlanConfigDto
import com.krachtix.commons.billing.BillingPlanConfigGateway
import com.krachtix.commons.billing.BillingPlanConfigWithMerchantDto
import com.krachtix.commons.cache.CacheNames
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class IdentityBillingPlanClient(
    private val restClient: RestClient,
    private val authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${integration.identity-ms.base-url}") private val identityMsBaseUrl: String,
    @Value("\${integration.identity-ms.oauth2.client-registration-id}") private val clientRegistrationId: String,
    @Value("\${integration.identity-ms.oauth2.principal}") private val principal: String
) : BillingPlanConfigGateway {

    @Cacheable(cacheNames = [CacheNames.BILLING_PLAN], key = "#merchantId.toString()", unless = "#result == null")
    override fun getActivePlanForMerchant(merchantId: UUID): BillingPlanConfigDto? {
        log.info { "Fetching active billing plan from identity-ms for merchantId=$merchantId" }

        val accessToken = getAccessToken()

        val response = restClient.get()
            .uri("$identityMsBaseUrl/api/internal/billing/plans/active?organizationId=$merchantId")
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .body(InternalBillingPlanResponse::class.java)
            ?: return null

        return BillingPlanConfigDto(
            publicId = response.publicId,
            name = response.name,
            platformFeeAmount = response.platformFeeAmount,
            perAccountFeeAmount = response.perAccountFeeAmount,
            perTransactionFeeAmount = response.perTransactionFeeAmount,
            maxChargeAmount = response.maxChargeAmount,
            currency = response.currency,
            billingCycle = response.billingCycle,
            status = response.status,
            effectiveFrom = response.effectiveFrom,
            effectiveUntil = response.effectiveUntil,
            subscriptionTier = response.subscriptionTier,
            stripePriceId = response.stripePriceId
        )
    }

    override fun getAllActivePlansWithMerchant(): List<BillingPlanConfigWithMerchantDto> {
        log.debug { "Fetching all active billing plans from identity-ms" }

        val accessToken = getAccessToken()

        val responses = restClient.get()
            .uri("$identityMsBaseUrl/api/internal/billing/plans/active/all")
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .body(object : ParameterizedTypeReference<List<InternalActivePlanWithOrgResponse>>() {})
            ?: return emptyList()

        return responses.mapNotNull { response ->
            response.merchantId?.let { merchantId ->
                BillingPlanConfigWithMerchantDto(
                    merchantId = merchantId,
                    plan = BillingPlanConfigDto(
                        publicId = response.plan.publicId,
                        name = response.plan.name,
                        platformFeeAmount = response.plan.platformFeeAmount,
                        perAccountFeeAmount = response.plan.perAccountFeeAmount,
                        perTransactionFeeAmount = response.plan.perTransactionFeeAmount,
                        maxChargeAmount = response.plan.maxChargeAmount,
                        currency = response.plan.currency,
                        billingCycle = response.plan.billingCycle,
                        status = response.plan.status,
                        effectiveFrom = response.plan.effectiveFrom,
                        effectiveUntil = response.plan.effectiveUntil,
                        subscriptionTier = response.plan.subscriptionTier,
                        stripePriceId = response.plan.stripePriceId
                    )
                )
            }
        }
    }

    private fun getAccessToken(): String {
        val authorizeRequest = OAuth2AuthorizeRequest
            .withClientRegistrationId(clientRegistrationId)
            .principal(principal)
            .build()

        return authorizedClientManager.authorize(authorizeRequest)?.accessToken?.tokenValue
            ?: throw IllegalStateException("Failed to obtain access token for identity-ms billing API")
    }
}

private data class InternalBillingPlanResponse(
    val publicId: UUID = UUID.randomUUID(),
    val organizationId: UUID? = null,
    val name: String = "",
    val platformFeeAmount: BigDecimal = BigDecimal.ZERO,
    val perAccountFeeAmount: BigDecimal = BigDecimal.ZERO,
    val perTransactionFeeAmount: BigDecimal = BigDecimal.ZERO,
    val maxChargeAmount: BigDecimal? = null,
    val currency: String = "USD",
    val billingCycle: String = "MONTHLY",
    val status: String = "ACTIVE",
    val effectiveFrom: Instant = Instant.now(),
    val effectiveUntil: Instant? = null,
    val subscriptionTier: String? = null,
    val stripePriceId: String? = null
)

private data class InternalActivePlanWithOrgResponse(
    val organizationId: UUID? = null,
    val merchantId: UUID? = null,
    val plan: InternalBillingPlanResponse = InternalBillingPlanResponse()
)
