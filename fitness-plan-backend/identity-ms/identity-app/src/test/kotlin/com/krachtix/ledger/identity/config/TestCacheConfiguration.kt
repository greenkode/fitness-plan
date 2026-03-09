package com.krachtix.identity.config

import com.krachtix.commons.cache.CacheNames
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestCacheConfiguration {

    @Bean
    @Primary
    fun testCacheManager(): CacheManager {
        return ConcurrentMapCacheManager(
            "rate-limit-config",
            "rate-limit-bucket",
            CacheNames.PROCESS,
            CacheNames.USER_SETTINGS,
            CacheNames.MERCHANT_CLIENT,
            CacheNames.OAUTH_USER,
            CacheNames.USER_ROLES,
            CacheNames.REGISTERED_CLIENT,
            CacheNames.KYC_USER,
            CacheNames.MERCHANT_DETAILS,
            CacheNames.USER_DETAILS,
            CacheNames.COUNTRIES
        )
    }
}
