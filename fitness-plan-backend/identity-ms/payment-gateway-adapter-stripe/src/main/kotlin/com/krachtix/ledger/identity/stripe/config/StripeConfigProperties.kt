package com.krachtix.identity.stripe.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "stripe")
data class StripeConfigProperties(
    val apiKey: String,
    val webhookSecret: String,
    val enabled: Boolean = false
)
