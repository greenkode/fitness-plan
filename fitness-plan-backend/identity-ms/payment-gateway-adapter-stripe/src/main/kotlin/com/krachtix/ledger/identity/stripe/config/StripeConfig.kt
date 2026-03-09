package com.krachtix.identity.stripe.config

import com.stripe.Stripe
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

@Configuration
@EnableConfigurationProperties(StripeConfigProperties::class)
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class StripeConfig(
    private val stripeConfigProperties: StripeConfigProperties
) {

    @PostConstruct
    fun init() {
        Stripe.apiKey = stripeConfigProperties.apiKey
        log.info { "Stripe API configured" }
    }
}
