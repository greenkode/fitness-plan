package com.krachtix.identity.core.auth.service

import com.hazelcast.core.HazelcastInstance
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Service
class UserTokenRevocationService(
    @Value("\${identity-ms.token.expiry:600}")
    private val accessTokenTtlSeconds: Long
) {

    @Autowired(required = false)
    private var hazelcastInstance: HazelcastInstance? = null

    companion object {
        const val REVOCATION_MAP = "user-token-revocation"
    }

    fun revokeUserAccessTokens(userId: UUID) {
        val map = hazelcastInstance?.getMap<String, Long>(REVOCATION_MAP)
        if (map == null) {
            log.warn { "Hazelcast not available, skipping access token revocation for user: $userId" }
            return
        }
        map.put(userId.toString(), Instant.now().epochSecond, accessTokenTtlSeconds, TimeUnit.SECONDS)
        log.info { "Access tokens revoked for user: $userId (revokedBefore: ${Instant.now().epochSecond})" }
    }
}
