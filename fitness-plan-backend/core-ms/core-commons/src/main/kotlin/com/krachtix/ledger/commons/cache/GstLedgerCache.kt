package com.krachtix.commons.cache

import java.time.Duration
import java.util.concurrent.TimeUnit

enum class GstLedgerCache(val cacheName: String, val ttl: Long, val timeUnit: TimeUnit) {

    ACCOUNT(CacheName.Account.name, 60, TimeUnit.MINUTES);

    fun computeTtl(ttlTimeUnit: TimeUnit, ttl: Long): Duration {
        return when (ttlTimeUnit) {
            TimeUnit.SECONDS -> Duration.ofSeconds(ttl)
            TimeUnit.MINUTES -> Duration.ofMinutes(ttl)
            TimeUnit.HOURS -> Duration.ofHours(ttl)
            TimeUnit.DAYS -> Duration.ofDays(ttl)
            else -> Duration.ofSeconds(60)
        }
    }

    enum class CacheName {
        Account
    }
}