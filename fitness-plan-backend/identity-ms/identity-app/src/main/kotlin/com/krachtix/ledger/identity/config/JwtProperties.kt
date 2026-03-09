package com.krachtix.identity.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    var privateKeyPath: String = "",
    var publicKeyPath: String = "",
    var issuer: String = "http://localhost:9202",
    var expiration: Long = 3600,
    var audience: String = "core-ms-client"
)