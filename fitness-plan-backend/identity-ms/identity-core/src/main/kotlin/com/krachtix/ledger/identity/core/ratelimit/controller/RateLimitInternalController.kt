package com.krachtix.identity.core.ratelimit.controller

import com.krachtix.identity.commons.dto.RateLimitConfigDto
import com.krachtix.identity.commons.dto.RateLimitConfigsResponse
import com.krachtix.identity.core.ratelimit.service.RateLimitConfigService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/internal/rate-limits")
@PreAuthorize("hasAuthority('SCOPE_internal:read')")
class RateLimitInternalController(
    private val rateLimitConfigService: RateLimitConfigService
) {

    @GetMapping
    fun getAllActiveConfigs(): RateLimitConfigsResponse {
        log.debug { "Fetching all active rate limit configurations" }

        val configs = rateLimitConfigService.getAllActiveConfigs()
            .map { config ->
                RateLimitConfigDto(
                    methodName = config.methodName,
                    subscriptionTier = config.subscriptionTier.name,
                    scope = config.scope.name,
                    capacity = config.capacity,
                    timeValue = config.timeValue,
                    timeUnit = config.timeUnit
                )
            }

        log.debug { "Returning ${configs.size} rate limit configurations" }
        return RateLimitConfigsResponse(configs = configs)
    }
}
