package com.krachtix.ratelimit

import com.krachtix.ratelimit.service.DynamicRateLimitResolver
import com.krachtix.ratelimit.service.RateLimitScope
import com.krachtix.commons.tenant.TenantContext
import com.hazelcast.core.HazelcastInstance
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.grid.hazelcast.HazelcastProxyManager
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(prefix = "bucket4j", name = ["enabled"], havingValue = "true")
class RateLimitInterceptor(
    private val dynamicRateLimitResolver: DynamicRateLimitResolver,
    private val hazelcastInstance: HazelcastInstance
) : HandlerInterceptor {

    private val proxyManagers = ConcurrentHashMap<String, ProxyManager<String>>()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val methodName = extractMethodName(request)
        val resolution = dynamicRateLimitResolver.resolveForCurrentUser(methodName, RateLimitScope.ORGANIZATION)

        val proxyManager = getOrCreateProxyManager(methodName)
        val configSupplier = Supplier {
            BucketConfiguration.builder()
                .addLimit(resolution.bandwidth)
                .build()
        }

        val bucket: Bucket = proxyManager.builder().build(resolution.bucketKey, configSupplier)

        val probe = bucket.tryConsumeAndReturnRemaining(1)
        if (probe.isConsumed) {
            response.setHeader("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
            return true
        }

        log.warn { "Rate limit exceeded for bucket: ${resolution.bucketKey}" }
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.setHeader("X-Rate-Limit-Retry-After-Seconds", (probe.nanosToWaitForRefill / 1_000_000_000).toString())
        response.writer.write("""{"error": "Rate limit exceeded", "message": "Too many requests. Please try again later."}""")
        return false
    }

    private fun getOrCreateProxyManager(methodName: String): ProxyManager<String> {
        return proxyManagers.computeIfAbsent(methodName) {
            val mapName = "rate-limit-$methodName"
            HazelcastProxyManager(hazelcastInstance.getMap(mapName))
        }
    }

    private fun extractMethodName(request: HttpServletRequest): String {
        val path = request.requestURI
        return when {
            path.startsWith("/api/transactions") -> "transaction-create"
            path.startsWith("/api/reports") -> "report-generate"
            path.startsWith("/api/accounts") -> "account-operation"
            path.startsWith("/api/incentives") -> "incentive-operation"
            else -> "api-request"
        }
    }
}
