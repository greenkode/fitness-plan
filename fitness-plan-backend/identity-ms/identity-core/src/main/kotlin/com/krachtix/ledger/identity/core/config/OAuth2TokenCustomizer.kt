package com.krachtix.identity.core.config

import com.krachtix.identity.core.config.JwtClaimName.AUTHORITIES
import com.krachtix.identity.core.config.JwtClaimName.BILLING_TIER
import com.krachtix.identity.core.config.JwtClaimName.CLIENT_TYPE
import com.krachtix.identity.core.config.JwtClaimName.ENVIRONMENT
import com.krachtix.identity.core.config.JwtClaimName.ENVIRONMENT_CONFIG
import com.krachtix.identity.core.config.JwtClaimName.ENVIRONMENT_PREFERENCE
import com.krachtix.identity.core.config.JwtClaimName.MERCHANT_ENVIRONMENT_MODE
import com.krachtix.identity.core.config.JwtClaimName.MERCHANT_ID
import com.krachtix.identity.core.config.JwtClaimName.ORGANIZATION_ID
import com.krachtix.identity.core.config.JwtClaimName.ORGANIZATION_STATUS
import com.krachtix.identity.core.config.JwtClaimName.REALM_ACCESS
import com.krachtix.identity.core.config.JwtClaimName.RESOURCE_ACCESS
import com.krachtix.identity.core.config.JwtClaimName.ROLES
import com.krachtix.identity.core.config.JwtClaimName.SETUP_COMPLETED
import com.krachtix.identity.core.config.JwtClaimName.TOTP_ENABLED
import com.krachtix.identity.core.config.JwtClaimName.TWO_FACTOR_LAST_VERIFIED
import com.krachtix.identity.core.config.JwtClaimName.TYPE
import com.krachtix.identity.core.auth.service.JwtTokenService
import com.krachtix.identity.core.entity.EnvironmentMode
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.repository.OAuthUserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.jwt.JwtClaimNames as StandardJwtClaimNames
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import java.util.UUID

private val log = KotlinLogging.logger {}

@Configuration
class OAuth2TokenCustomizerConfig {

    @Bean
    fun jwtTokenCustomizer(
        clientRepository: OAuthRegisteredClientRepository,
        userRepository: OAuthUserRepository,
        organizationRepository: OrganizationRepository
    ): OAuth2TokenCustomizer<JwtEncodingContext> {
        return OAuth2TokenCustomizer { context ->
            log.info { "Customizing token for grant type: ${context.authorizationGrantType?.value}" }

            addJtiClaim(context)
            normalizeScopes(context)

            when {
                context.authorizationGrantType == AuthorizationGrantType.CLIENT_CREDENTIALS -> {
                    customizeClientCredentialsToken(context, clientRepository)
                }

                context.authorizationGrantType == JwtTokenService.TWO_FACTOR_AUTH_GRANT_TYPE -> {
                    customizeTwoFactorToken(context)
                }

                context.tokenType == OAuth2TokenType.ACCESS_TOKEN -> {
                    customizeUserAccessToken(context, userRepository, clientRepository, organizationRepository)
                }
            }
        }
    }

    private fun addJtiClaim(context: JwtEncodingContext) {
        val jti = UUID.randomUUID().toString()
        context.claims.claim(StandardJwtClaimNames.JTI, jti)
        log.debug { "Added JTI claim: $jti for token type: ${context.tokenType?.value}" }
    }

    private fun normalizeScopes(context: JwtEncodingContext) {
        val scopes = context.authorizedScopes
        scopes.takeIf { it.isNotEmpty() }?.let {
            context.claims.claim("scope", it.joinToString(" "))
        }
    }

    private fun customizeClientCredentialsToken(
        context: JwtEncodingContext,
        clientRepository: OAuthRegisteredClientRepository
    ) {
        val clientId = context.registeredClient?.clientId
        log.info { "Customizing client credentials token for client: $clientId" }

        runCatching {
            val client = clientId?.let { clientRepository.findByClientId(it) }

            if (client?.isKnowledgeBaseClient() == true) {
                context.claims.claim("knowledge_base_id", client.knowledgeBaseId)
                context.claims.claim("organization_id", client.organizationId.toString())
                context.claims.claim(CLIENT_TYPE.value, "kb_api")
                context.claims.claim(TYPE.value, "KB_API")
                log.info { "Added KB claims for client: $clientId, kb: ${client.knowledgeBaseId}, org: ${client.organizationId}" }
                return@runCatching
            }

            client?.let {
                context.claims.claim(MERCHANT_ID.value, it.id)
                context.claims.claim(ENVIRONMENT.value, it.environmentMode.name)
                it.plan.let { tier -> context.claims.claim(BILLING_TIER.value, tier.name) }
                log.info { "Added merchant_id: ${it.id} and environment: ${it.environmentMode} for client: $clientId" }
            } ?: context.claims.claim(ENVIRONMENT.value, EnvironmentMode.SANDBOX.name)

            context.claims.claim(TYPE.value, "BUSINESS")
            context.claims.claim(CLIENT_TYPE.value, "service")
        }.onFailure { e ->
            log.error(e) { "Failed to fetch client data for: $clientId" }
            context.claims.claim(ENVIRONMENT.value, EnvironmentMode.SANDBOX.name)
        }
    }

    private fun customizeUserAccessToken(
        context: JwtEncodingContext,
        userRepository: OAuthUserRepository,
        clientRepository: OAuthRegisteredClientRepository,
        organizationRepository: OrganizationRepository
    ) {
        val principal = context.getPrincipal<org.springframework.security.core.Authentication>()
        val username = principal?.name
            ?: return log.warn { "Username is null in token customization context" }

        log.info { "Customizing user access token for user: $username" }

        runCatching {
            val user = userRepository.findByUsername(username)
                ?: return run {
                    log.warn { "User not found: $username" }
                    context.claims.claim(ENVIRONMENT.value, EnvironmentMode.SANDBOX.name)
                }

            context.claims.subject(user.id.toString())
            log.info { "Set token subject to user ID: ${user.id}" }

            val merchant = user.merchantId?.let { clientRepository.findById(it).orElse(null) }
            val organization = user.merchantId?.let { organizationRepository.findByIdWithProperties(it).orElse(null) }
            val userEnvironmentPreference = user.environmentPreference
            val merchantEnvironmentMode = merchant?.environmentMode ?: EnvironmentMode.SANDBOX
            val effectiveEnvironment = merchantEnvironmentMode
                .takeIf { it == EnvironmentMode.PRODUCTION }
                ?.let { userEnvironmentPreference }
                ?: merchantEnvironmentMode

            log.info {
                "Adding environment claims for user $username: " +
                        "effective=$effectiveEnvironment, " +
                        "userPref=$userEnvironmentPreference, " +
                        "merchantMode=$merchantEnvironmentMode"
            }

            val userAuthorities = principal.authorities.map { it.authority }
            val realmAccess = mapOf(ROLES.value to (listOf("offline_access", "uma_authorization", "default-roles-akuid") + userAuthorities))
            val resourceAccess = mapOf("account" to mapOf(ROLES.value to listOf("manage-account", "manage-account-links", "view-profile")))

            user.merchantId?.let { context.claims.claim(MERCHANT_ID.value, it.toString()) }
            user.organizationId?.let { context.claims.claim(ORGANIZATION_ID.value, it.toString()) }

            val setupCompleted = organization?.getProperty(OrganizationPropertyName.SETUP_COMPLETED) == "true"
            context.claims.claim(SETUP_COMPLETED.value, setupCompleted)
            val orgStatus = organization?.status ?: merchant?.status
            orgStatus?.name?.let { context.claims.claim(ORGANIZATION_STATUS.value, it) }

            organization?.plan?.let { context.claims.claim(BILLING_TIER.value, it.name) }

            val environmentConfig = mapOf(
                ENVIRONMENT.value to effectiveEnvironment.name,
                ENVIRONMENT_PREFERENCE.value to userEnvironmentPreference.name,
                MERCHANT_ENVIRONMENT_MODE.value to merchantEnvironmentMode.name
            )

            context.claims.claim(ENVIRONMENT_CONFIG.value, environmentConfig)
            context.claims.claim(REALM_ACCESS.value, realmAccess)
            context.claims.claim(RESOURCE_ACCESS.value, resourceAccess)
            context.claims.claim(AUTHORITIES.value, userAuthorities)
            context.claims.claim(TOTP_ENABLED.value, user.totpEnabled)
            user.twoFactorLastVerified?.let {
                context.claims.claim(TWO_FACTOR_LAST_VERIFIED.value, it.epochSecond)
            }
        }.onFailure { e ->
            log.error(e) { "Failed to fetch user data for: $username" }
            context.claims.claim(ENVIRONMENT.value, EnvironmentMode.SANDBOX.name)
        }
    }

    private fun customizeTwoFactorToken(context: JwtEncodingContext) {
        val principal = context.getPrincipal<org.springframework.security.core.Authentication>()
        val authorities = principal?.authorities?.map { it.authority } ?: listOf("ROLE_TWO_FACTOR_AUTH")

        context.claims.claim(AUTHORITIES.value, authorities)
        context.claims.claim(TYPE.value, "TWO_FACTOR_AUTH")

        log.info { "Customized restricted 2FA token with authorities: $authorities" }
    }

}