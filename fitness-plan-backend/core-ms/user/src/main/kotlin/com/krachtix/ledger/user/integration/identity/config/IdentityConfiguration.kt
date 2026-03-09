package com.krachtix.user.integration.identity.config

import com.krachtix.user.dao.AccessTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper

@Configuration
class IdentityConfiguration(@param:Value("\${integration.identity-ms.base-url}") private val baseUrl: String) {

    @Bean("identityRestClient")
    fun identityRestClient(
        accessTokenRepository: AccessTokenRepository,
        clientManager: OAuth2AuthorizedClientManager,
        objectMapper: JsonMapper
    ): RestClient {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestInterceptor(IdentityClientAuthenticationInterceptor(accessTokenRepository, clientManager))
            .configureMessageConverters { it.registerDefaults().withJsonConverter(JacksonJsonHttpMessageConverter(objectMapper)) }
            .build()
    }
}