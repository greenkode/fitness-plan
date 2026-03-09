package com.krachtix.user.integration.identity.config

import com.krachtix.user.AccessTokenType
import com.krachtix.user.dao.AccessTokenEntity
import com.krachtix.user.dao.AccessTokenRepository
import com.krachtix.user.domain.AccessToken
import com.krachtix.user.integration.identity.ID_INTEGRATION_CODE
import com.krachtix.user.integration.identity.ID_INTEGRATION_RESOURCE
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import java.time.Instant

class IdentityClientAuthenticationInterceptor(
    private val accessTokenRepository: AccessTokenRepository,
    private val clientManager: OAuth2AuthorizedClientManager,

    ) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {

        val authorizeRequest: OAuth2AuthorizeRequest = OAuth2AuthorizeRequest
            .withClientRegistrationId("identity-ms")
            .principal("identity-ms")
            .build()

        val token =
            getAccessToken(authorizeRequest)

        request.headers.setBearerAuth(token.accessToken)

        return execution.execute(request, body)
    }

    @Synchronized
    private fun getAccessToken(authorizeRequest: OAuth2AuthorizeRequest): AccessToken {
        val token =
            accessTokenRepository.findFirstByInstitutionAndResourceAndExpiryAfterOrderByExpiryDesc(
                ID_INTEGRATION_RESOURCE,
                ID_INTEGRATION_RESOURCE, Instant.now()
            )?.toDomain()

                ?: run {

                    val response = clientManager.authorize(authorizeRequest)

                    response?.let {

                        accessTokenRepository.save(
                            AccessTokenEntity(
                                AccessTokenType.IDENTITY_INTEGRATION,
                                response.accessToken.expiresAt!!,
                                response.accessToken.tokenValue,
                                response.refreshToken?.tokenValue,
                                ID_INTEGRATION_RESOURCE,
                                ID_INTEGRATION_CODE
                            )
                        ).toDomain()

                    }
                        ?: throw BadCredentialsException("Unable to get authentication token for integration: $ID_INTEGRATION_CODE")
                }
        return token
    }
}