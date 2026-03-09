package com.krachtix.identity.core.ratelimit.controller

import com.krachtix.identity.core.organization.entity.SubscriptionTier
import com.krachtix.identity.core.ratelimit.domain.RateLimitConfig
import com.krachtix.identity.core.ratelimit.domain.RateLimitScope
import com.krachtix.identity.core.ratelimit.service.RateLimitConfigService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class RateLimitInternalControllerTest {

    @Mock
    private lateinit var rateLimitConfigService: RateLimitConfigService

    @InjectMocks
    private lateinit var controller: RateLimitInternalController

    @Test
    fun `getAllActiveConfigs returns empty list when no configs exist`() {
        `when`(rateLimitConfigService.getAllActiveConfigs()).thenReturn(emptyList())

        val response = controller.getAllActiveConfigs()

        assertEquals(0, response.configs.size)
    }

    @Test
    fun `getAllActiveConfigs returns all active configs`() {
        val configs = listOf(
            RateLimitConfig(
                methodName = "api-request",
                subscriptionTier = SubscriptionTier.TRIAL,
                scope = RateLimitScope.ORGANIZATION,
                capacity = 100,
                timeValue = 1,
                timeUnit = ChronoUnit.HOURS
            ),
            RateLimitConfig(
                methodName = "api-request",
                subscriptionTier = SubscriptionTier.PROFESSIONAL,
                scope = RateLimitScope.ORGANIZATION,
                capacity = 1000,
                timeValue = 1,
                timeUnit = ChronoUnit.HOURS
            )
        )
        `when`(rateLimitConfigService.getAllActiveConfigs()).thenReturn(configs)

        val response = controller.getAllActiveConfigs()

        assertEquals(2, response.configs.size)
        assertEquals("api-request", response.configs[0].methodName)
        assertEquals("TRIAL", response.configs[0].subscriptionTier)
        assertEquals("ORGANIZATION", response.configs[0].scope)
        assertEquals(100, response.configs[0].capacity)
        assertEquals(1, response.configs[0].timeValue)
        assertEquals(ChronoUnit.HOURS, response.configs[0].timeUnit)
    }

    @Test
    fun `getAllActiveConfigs maps all fields correctly`() {
        val config = RateLimitConfig(
            methodName = "transaction-create",
            subscriptionTier = SubscriptionTier.ENTERPRISE,
            scope = RateLimitScope.INDIVIDUAL,
            capacity = 500,
            timeValue = 30,
            timeUnit = ChronoUnit.MINUTES
        )
        `when`(rateLimitConfigService.getAllActiveConfigs()).thenReturn(listOf(config))

        val response = controller.getAllActiveConfigs()

        assertEquals(1, response.configs.size)
        val dto = response.configs[0]
        assertEquals("transaction-create", dto.methodName)
        assertEquals("ENTERPRISE", dto.subscriptionTier)
        assertEquals("INDIVIDUAL", dto.scope)
        assertEquals(500, dto.capacity)
        assertEquals(30, dto.timeValue)
        assertEquals(ChronoUnit.MINUTES, dto.timeUnit)
    }
}
