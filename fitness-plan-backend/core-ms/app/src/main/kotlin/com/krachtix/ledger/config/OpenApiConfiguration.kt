package com.krachtix.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.money.MonetaryAmount

data class MoneySchema(
    val value: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val currency: String = "USD"
)

@Configuration
@ConditionalOnProperty(
    name = ["springdoc.api-docs.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class OpenApiConfiguration {

    init {
        org.springdoc.core.utils.SpringDocUtils.getConfig()
            .replaceWithClass(MonetaryAmount::class.java, MoneySchema::class.java)
            .replaceWithClass(org.javamoney.moneta.Money::class.java, MoneySchema::class.java)
    }

    companion object {
        private val API_DESCRIPTION = """
# Krachtix API

Welcome to the Krachtix API documentation. This API provides comprehensive financial services including account management, transactions, and balance operations.

## Overview

The Krachtix API enables merchants and partners to integrate robust account and transaction management functionality into their platforms.

## Authentication

All API endpoints require valid JWT authentication. Use OAuth2 Client Credentials flow to obtain access tokens.

**Token Endpoint**: `POST {identity-server}/oauth2/token`

## Response Codes

| Code | Description |
|------|-------------|
| 2000 | SUCCESS - Operation completed successfully |
| 4000 | INVALID_REQUEST - Invalid request format |
| 4003 | INVALID_ACCOUNT - Invalid account |
| 4007 | INSUFFICIENT_FUNDS - Insufficient balance |
| 5000 | SYSTEM_ERROR - Internal system error |
        """.trimIndent()
    }

    @Bean
    @Profile("sandbox", "local")
    fun developmentOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Krachtix API - Development")
                    .version("1.0.0")
                    .description(API_DESCRIPTION)
                    .contact(
                        Contact()
                            .name("Krachtix Team")
                            .email("support@krachtix.com")
                    )
                    .license(
                        License()
                            .name("Proprietary")
                    )
            )
            .addServersItem(
                Server()
                    .url("http://localhost:9201")
                    .description("Local Development")
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT access token obtained from OAuth2 token endpoint")
                    )
                    .addSecuritySchemes(
                        "oauth2",
                        SecurityScheme()
                            .type(SecurityScheme.Type.OAUTH2)
                            .description("OAuth2 Client Credentials authentication")
                            .flows(
                                OAuthFlows()
                                    .clientCredentials(
                                        OAuthFlow()
                                            .tokenUrl("http://localhost:9202/oauth2/token")
                                            .scopes(
                                                Scopes()
                                                    .addString("openid", "OpenID Connect scope")
                                                    .addString("internal:read", "Internal read access")
                                            )
                                    )
                            )
                    )
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
    }

    @Bean
    @Profile("production")
    fun productionOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Krachtix API")
                    .version("1.0.0")
                    .description(API_DESCRIPTION)
                    .contact(
                        Contact()
                            .name("Krachtix Support")
                            .email("support@krachtix.com")
                    )
                    .license(
                        License()
                            .name("Commercial License")
                    )
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT access token obtained from OAuth2 token endpoint")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
    }

    @Bean
    @Profile("sandbox", "local")
    fun accountApiGroup(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("accounts")
            .displayName("Account Management API")
            .pathsToMatch("/api/accounts/**")
            .build()
    }

    @Bean
    @Profile("sandbox", "local")
    fun allApiGroup(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("all")
            .displayName("All APIs")
            .pathsToMatch("/**")
            .build()
    }

}
