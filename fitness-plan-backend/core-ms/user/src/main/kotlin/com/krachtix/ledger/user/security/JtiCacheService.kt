package com.krachtix.user.security

import com.hazelcast.core.HazelcastInstance
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Service
class JtiCacheService(
    @Value("\${security.jwt.jti-cache.ttl-seconds:900}")
    private val ttlSeconds: Long
) {

    @Autowired(required = false)
    private var hazelcastInstance: HazelcastInstance? = null

    companion object {
        private const val JTI_CACHE_NAME = "jti-replay-cache"
    }

    fun isJtiUsed(jti: String): Boolean {
        val cache = getCache() ?: return false
        return cache.containsKey(jti)
    }

    fun markJtiAsUsed(jti: String): Boolean {
        val cache = getCache() ?: return true

        val existingValue = cache.putIfAbsent(jti, System.currentTimeMillis(), ttlSeconds, TimeUnit.SECONDS)
        if (existingValue != null) {
            log.warn { "JTI replay attempt detected: $jti" }
            return false
        }

        log.debug { "JTI marked as used: $jti (TTL: ${ttlSeconds}s)" }
        return true
    }

    private fun getCache(): com.hazelcast.map.IMap<String, Long>? {
        return hazelcastInstance?.getMap(JTI_CACHE_NAME)
    }
}
