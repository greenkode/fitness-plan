package com.krachtix.identity.core.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import tools.jackson.databind.json.JsonMapper
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.web.client.RestClient

@Configuration
class OAuth2ClientConfig {

    private val log = KotlinLogging.logger {}

    @Bean
    @Primary
    fun authorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientRepository: OAuth2AuthorizedClientRepository
    ): OAuth2AuthorizedClientManager {
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build()

        val authorizedClientManager = DefaultOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientRepository
        )
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)

        return authorizedClientManager
    }

    @Bean
    fun authorizedClientService(
        clientRegistrationRepository: ClientRegistrationRepository
    ): OAuth2AuthorizedClientService {
        return InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository)
    }

    @Bean
    fun serviceAuthorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService
    ): AuthorizedClientServiceOAuth2AuthorizedClientManager {
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build()

        val clientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService
        )
        clientManager.setAuthorizedClientProvider(authorizedClientProvider)

        return clientManager
    }

    @Bean
    fun oauth2RestClient(
        @Value("\${core-ms.base-url}") coreMsBaseUrl: String,
        serviceAuthorizedClientManager: AuthorizedClientServiceOAuth2AuthorizedClientManager,
        objectMapper: JsonMapper
    ): RestClient {
        val oauth2Interceptor = ClientHttpRequestInterceptor { request, body, execution ->
            runCatching {
                val authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("identity-ms-client")
                    .principal("identity-ms-client")
                    .build()

                val authorizedClient = serviceAuthorizedClientManager.authorize(authorizeRequest)

                if (authorizedClient != null) {
                    val accessToken = authorizedClient.accessToken.tokenValue
                    request.headers.setBearerAuth(accessToken)
                } else {
                    log.error { "serviceAuthorizedClientManager.authorize() returned null for identity-ms-client" }
                }
            }.onFailure { e ->
                log.error(e) { "Failed to obtain OAuth2 token for identity-ms-client: ${e.message}" }
            }

            execution.execute(request, body)
        }

        return RestClient.builder()
            .baseUrl(coreMsBaseUrl)
            .requestInterceptor(oauth2Interceptor)
            .configureMessageConverters { it.registerDefaults().withJsonConverter(JacksonJsonHttpMessageConverter(objectMapper)) }
            .build()
    }
}
