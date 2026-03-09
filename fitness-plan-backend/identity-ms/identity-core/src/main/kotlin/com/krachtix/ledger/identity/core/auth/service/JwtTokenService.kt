package com.krachtix.identity.core.auth.service

import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.service.CustomUserDetails
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class JwtTokenService(
    private val tokenGenerator: OAuth2TokenGenerator<*>,
    private val authorizationServerSettings: AuthorizationServerSettings,
    private val clientRepository: OAuthRegisteredClientRepository,
    @Value("\${identity-ms.token.expiry:600}") private val tokenExpiry: Long,
    @Value("\${identity-ms.refresh-token.expiry:86400}") private val refreshTokenExpiry: Long
) {

    companion object {
        private val DEFAULT_SCOPES = setOf("openid", "profile", "email", "read")
        val TWO_FACTOR_AUTH_GRANT_TYPE = AuthorizationGrantType("two_factor_auth")
        private const val TWO_FACTOR_TOKEN_TTL_SECONDS = 300L
    }

    private fun getRegisteredClientForUser(user: OAuthUser): RegisteredClient {

        user.merchantId?.let { merchantId ->
            val dbClient = clientRepository.findByIdWithSettingsAndScopes(merchantId).orElse(null)

            if (dbClient != null) {
                val scopes = dbClient.scopes.map { it.name }.toSet().ifEmpty { DEFAULT_SCOPES }
                log.debug { "Using merchant client ${dbClient.clientId} for user ${user.username} with scopes: $scopes" }

                return RegisteredClient.withId(dbClient.id.toString())
                    .clientId(dbClient.clientId)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType("direct_login"))
                    .scopes { it.addAll(scopes) }
                    .tokenSettings(
                        TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofSeconds(tokenExpiry))
                            .refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenExpiry))
                            .build()
                    )
                    .build()
            }
        }

        log.warn { "No merchant client found for user ${user.username}, using fallback with default scopes" }
        return RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("user-direct-login")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType("direct_login"))
            .scopes { it.addAll(DEFAULT_SCOPES) }
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofSeconds(tokenExpiry))
                    .refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenExpiry))
                    .build()
            )
            .build()
    }

    fun generateToken(user: OAuthUser, userDetails: CustomUserDetails): String {
        val authentication = UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities
        )

        val registeredClient = getRegisteredClientForUser(user)

        val settings = authorizationServerSettings
        val authServerContext = AuthorizationServerContextHolder.getContext()
            ?: object : AuthorizationServerContext {
                override fun getAuthorizationServerSettings() = settings
                override fun getIssuer() = settings.issuer
            }

        val tokenContext = DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(authentication)
            .authorizationServerContext(authServerContext)
            .authorizationGrantType(AuthorizationGrantType("direct_login"))
            .authorizedScopes(registeredClient.scopes)
            .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .build()

        val generatedToken = tokenGenerator.generate(tokenContext)
            ?: throw IllegalStateException("Failed to generate access token")

        return generatedToken.tokenValue.toString()
    }

    fun generateRefreshToken(user: OAuthUser): String {
        val userDetails = CustomUserDetails(user)
        val authentication = UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities
        )

        val registeredClient = getRegisteredClientForUser(user)

        val settings = authorizationServerSettings
        val authServerContext = AuthorizationServerContextHolder.getContext()
            ?: object : AuthorizationServerContext {
                override fun getAuthorizationServerSettings() = settings
                override fun getIssuer() = settings.issuer
            }

        val tokenContext = DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(authentication)
            .authorizationServerContext(authServerContext)
            .authorizationGrantType(AuthorizationGrantType("direct_login"))
            .authorizedScopes(registeredClient.scopes)
            .tokenType(OAuth2TokenType.REFRESH_TOKEN)
            .build()

        val generatedToken = tokenGenerator.generate(tokenContext)
            ?: throw IllegalStateException("Failed to generate refresh token")

        return generatedToken.tokenValue.toString()
    }

    fun generateRestrictedTwoFactorToken(user: OAuthUser): String {
        val restrictedAuthorities = listOf(SimpleGrantedAuthority("ROLE_TWO_FACTOR_AUTH"))
        val restrictedUserDetails = CustomUserDetails(
            OAuthUser().apply {
                this.username = user.username
                this.password = ""
                this.authorities = mutableSetOf("ROLE_TWO_FACTOR_AUTH")
                this.enabled = user.enabled
                this.accountNonExpired = user.accountNonExpired
                this.accountNonLocked = user.accountNonLocked
                this.credentialsNonExpired = user.credentialsNonExpired
            }
        )

        val authentication = UsernamePasswordAuthenticationToken(
            restrictedUserDetails,
            null,
            restrictedAuthorities
        )

        val registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("two-factor-auth")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(TWO_FACTOR_AUTH_GRANT_TYPE)
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofSeconds(TWO_FACTOR_TOKEN_TTL_SECONDS))
                    .build()
            )
            .build()

        val settings = authorizationServerSettings
        val authServerContext = AuthorizationServerContextHolder.getContext()
            ?: object : AuthorizationServerContext {
                override fun getAuthorizationServerSettings() = settings
                override fun getIssuer() = settings.issuer
            }

        val tokenContext = DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(authentication)
            .authorizationServerContext(authServerContext)
            .authorizationGrantType(TWO_FACTOR_AUTH_GRANT_TYPE)
            .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .build()

        val generatedToken = tokenGenerator.generate(tokenContext)
            ?: throw IllegalStateException("Failed to generate restricted 2FA token")

        log.info { "Generated restricted 2FA token for user: ${user.username}" }
        return generatedToken.tokenValue.toString()
    }

    fun getTokenExpirySeconds(): Long = tokenExpiry

    fun getRefreshTokenExpirySeconds(): Long = refreshTokenExpiry
}
