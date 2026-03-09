package com.krachtix.identity.core.ratelimit.domain

import com.krachtix.identity.core.organization.entity.SubscriptionTier
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RateLimitConfigRepository : JpaRepository<RateLimitConfig, UUID> {

    @Cacheable(cacheNames = ["rate-limit-config"], key = "'rl_' + #methodName + '_' + #tier.name() + '_' + #scope.name()")
    fun findByMethodNameAndSubscriptionTierAndScopeAndActiveTrue(
        methodName: String,
        tier: SubscriptionTier,
        scope: RateLimitScope
    ): RateLimitConfig?

    @Cacheable(cacheNames = ["rate-limit-config"], key = "'rl_all_' + #methodName + '_' + #scope.name()")
    fun findByMethodNameAndScopeAndActiveTrue(methodName: String, scope: RateLimitScope): List<RateLimitConfig>

    fun findAllByActiveTrue(): List<RateLimitConfig>
}
