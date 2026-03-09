package com.krachtix.ratelimit.service

import com.krachtix.ratelimit.client.RateLimitConfigClient
import com.krachtix.ratelimit.client.RateLimitConfigDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

enum class RateLimitScope {
    INDIVIDUAL,
    ORGANIZATION
}

enum class SubscriptionTier {
    TRIAL,
    STARTER,
    PROFESSIONAL,
    ENTERPRISE
}

@Service
class RateLimitConfigService(
    private val rateLimitConfigClient: RateLimitConfigClient
) {

    private val configCache = ConcurrentHashMap<String, RateLimitConfigDto>()
    private var lastRefresh: Long = 0

    @Cacheable(cacheNames = ["rate-limit-configs"], key = "'all_configs'")
    fun getAllConfigs(): List<RateLimitConfigDto> {
        refreshCacheIfNeeded()
        return configCache.values.toList()
    }

    fun getConfig(methodName: String, tier: SubscriptionTier, scope: RateLimitScope): RateLimitConfigDto? {
        refreshCacheIfNeeded()
        val key = buildCacheKey(methodName, tier.name, scope.name)
        return configCache[key]
    }

    @Scheduled(fixedDelay = 300000)
    fun refreshCache() {
        log.debug { "Refreshing rate limit config cache" }
        loadConfigs()
    }

    private fun refreshCacheIfNeeded() {
        if (configCache.isEmpty() || System.currentTimeMillis() - lastRefresh > 300000) {
            loadConfigs()
        }
    }

    private fun loadConfigs() {
        runCatching {
            val configs = rateLimitConfigClient.fetchAllConfigs()
            configCache.clear()
            configs.forEach { config ->
                val tier = config.subscriptionTier ?: return@forEach
                val scope = config.scope ?: return@forEach
                val key = buildCacheKey(config.methodName, tier, scope)
                configCache[key] = config
            }
            lastRefresh = System.currentTimeMillis()
            log.info { "Loaded ${configs.size} rate limit configurations from identity-ms" }
        }.onFailure { ex ->
            log.error(ex) { "Failed to load rate limit configurations" }
        }
    }

    private fun buildCacheKey(methodName: String, tier: String, scope: String): String {
        return "$methodName:$tier:$scope"
    }
}
