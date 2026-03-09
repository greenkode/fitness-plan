package com.krachtix.ratelimit.service

import com.krachtix.ratelimit.client.RateLimitConfigDto
import com.krachtix.commons.tenant.TenantContext
import com.krachtix.commons.user.UserGateway
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Refill
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Duration

private val log = KotlinLogging.logger {}

data class RateLimitResolution(
    val bandwidth: Bandwidth,
    val bucketKey: String,
    val scope: RateLimitScope
)

@Component
class DynamicRateLimitResolver(
    private val rateLimitConfigService: RateLimitConfigService,
    private val userGateway: UserGateway
) {

    fun resolveForOrganization(methodName: String, merchantId: String): RateLimitResolution {
        val tier = getMerchantSubscriptionTier(merchantId)
        val config = rateLimitConfigService.getConfig(methodName, tier, RateLimitScope.ORGANIZATION)
            ?: return RateLimitResolution(
                bandwidth = defaultBandwidth(methodName),
                bucketKey = "org:$methodName:$merchantId",
                scope = RateLimitScope.ORGANIZATION
            )

        log.debug { "Resolved rate limit for $methodName, tier $tier, scope ORGANIZATION: capacity=${config.capacity}, time=${config.timeValue} ${config.timeUnit}" }

        return RateLimitResolution(
            bandwidth = createBandwidth(config),
            bucketKey = "org:$methodName:$merchantId",
            scope = RateLimitScope.ORGANIZATION
        )
    }

    fun resolveForIndividual(methodName: String, userId: String, merchantId: String): RateLimitResolution {
        val tier = getMerchantSubscriptionTier(merchantId)
        val config = rateLimitConfigService.getConfig(methodName, tier, RateLimitScope.INDIVIDUAL)
            ?: return RateLimitResolution(
                bandwidth = defaultBandwidth(methodName),
                bucketKey = "user:$methodName:$userId",
                scope = RateLimitScope.INDIVIDUAL
            )

        log.debug { "Resolved rate limit for $methodName, tier $tier, scope INDIVIDUAL: capacity=${config.capacity}, time=${config.timeValue} ${config.timeUnit}" }

        return RateLimitResolution(
            bandwidth = createBandwidth(config),
            bucketKey = "user:$methodName:$userId",
            scope = RateLimitScope.INDIVIDUAL
        )
    }

    fun resolveForCurrentUser(methodName: String, scope: RateLimitScope): RateLimitResolution {
        val merchantId = TenantContext.getMerchantIdOrNull()?.toString()
        val userId = userGateway.getLoggedInUserId()?.toString()

        return when {
            merchantId == null -> RateLimitResolution(
                bandwidth = defaultBandwidth(methodName),
                bucketKey = "anonymous:$methodName",
                scope = scope
            )
            scope == RateLimitScope.ORGANIZATION -> resolveForOrganization(methodName, merchantId)
            userId != null -> resolveForIndividual(methodName, userId, merchantId)
            else -> RateLimitResolution(
                bandwidth = defaultBandwidth(methodName),
                bucketKey = "org:$methodName:$merchantId",
                scope = RateLimitScope.ORGANIZATION
            )
        }
    }

    fun resolveByIdentifier(methodName: String, identifier: String, merchantId: String, scope: RateLimitScope): RateLimitResolution {
        return when (scope) {
            RateLimitScope.ORGANIZATION -> resolveForOrganization(methodName, merchantId)
            RateLimitScope.INDIVIDUAL -> resolveForIndividual(methodName, identifier, merchantId)
        }
    }

    private fun getMerchantSubscriptionTier(merchantId: String): SubscriptionTier {
        return SubscriptionTier.TRIAL
    }

    private fun createBandwidth(config: RateLimitConfigDto): Bandwidth {
        val duration = Duration.of(config.timeValue.toLong(), config.timeUnit)
        return Bandwidth.classic(
            config.capacity.toLong(),
            Refill.intervally(config.capacity.toLong(), duration)
        )
    }

    private fun defaultBandwidth(methodName: String): Bandwidth {
        log.warn { "No rate limit config found for method: $methodName, using default" }
        return when (methodName) {
            "api-request" -> Bandwidth.classic(100, Refill.intervally(100, Duration.ofHours(1)))
            "transaction-create" -> Bandwidth.classic(50, Refill.intervally(50, Duration.ofMinutes(1)))
            "report-generate" -> Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(5)))
            else -> Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)))
        }
    }
}
