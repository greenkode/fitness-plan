package com.krachtix.ratelimit.config

import com.krachtix.ratelimit.RateLimitInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@ConditionalOnProperty(prefix = "bucket4j", name = ["enabled"], havingValue = "true")
class RateLimitWebConfig : WebMvcConfigurer {

    @Autowired
    private lateinit var rateLimitInterceptor: RateLimitInterceptor

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/actuator/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/webhooks/**"
            )
    }
}
