package com.krachtix.identity.core.oauth

import com.krachtix.commons.dto.Email
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.entity.OAuthClientSettingName
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.entity.OAuthProvider
import com.krachtix.identity.core.entity.OAuthProviderAccount
import com.krachtix.identity.core.entity.OAuthRegisteredClient
import com.krachtix.identity.core.entity.OAuthTokenSettingName
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.OrganizationStatus
import com.krachtix.identity.core.entity.RegistrationSource
import com.krachtix.commons.kyc.TrustLevel
import com.krachtix.identity.core.entity.UserType
import com.krachtix.identity.core.organization.entity.Organization
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.repository.OAuthProviderAccountRepository
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.BusinessEmailValidationService
import com.krachtix.identity.core.service.OAuthClientConfigService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class CustomOidcUserService(
    private val userRepository: OAuthUserRepository,
    private val providerAccountRepository: OAuthProviderAccountRepository,
    private val oauthClientRepository: OAuthRegisteredClientRepository,
    private val organizationRepository: OrganizationRepository,
    private val oauthClientConfigService: OAuthClientConfigService,
    private val businessEmailValidationService: BusinessEmailValidationService,
    private val passwordEncoder: PasswordEncoder,
    private val messageService: MessageService,
    private val processGateway: ProcessGateway
) : OidcUserService() {

    @Transactional
    override fun loadUser(userRequest: OidcUserRequest): OidcUser =
        runCatching {
            val oidcUser = super.loadUser(userRequest)
            val registrationId = userRequest.clientRegistration.registrationId

            log.info { "OIDC login attempt from provider: $registrationId" }

            val provider = parseProvider(registrationId)
            val providerUserId = oidcUser.subject
                ?: throw oauthError("missing_user_id", messageService.getMessage("oauth.error.missing_user_id"))
            val email = oidcUser.email?.lowercase()
                ?: throw oauthError("email_required", messageService.getMessage("oauth.error.email_required"))

            businessEmailValidationService.validateBusinessEmail(email)

            val user = findUserByProviderAccount(provider, providerUserId)
                ?: findUserByEmailAndLinkProvider(email, provider, providerUserId)
                ?: resolveUserForDomain(email, provider, providerUserId, oidcUser)

            OAuth2UserPrincipal(
                oauth2User = oidcUser,
                internalUser = user,
                provider = provider
            )
        }.getOrElse { e ->
            log.error(e) { "OIDC authentication failed: ${e.message}" }
            throw (e as? OAuth2AuthenticationException)
                ?: oauthError("authentication_failed", e.message ?: "Authentication failed")
        }

    private fun resolveUserForDomain(
        email: String,
        provider: OAuthProvider,
        providerUserId: String,
        oidcUser: OidcUser
    ): OAuthUser {
        val domain = email.substringAfter("@")

        return oauthClientRepository.findByDomain(domain)?.let { client ->
            val superAdminEmail = findSuperAdminEmail(client.id)
            throw oauthError(
                "invitation_required",
                messageService.getMessage("oauth.error.domain_exists", superAdminEmail)
            )
        } ?: run {
            val (organization, oauthClient) = createOrganizationAndOAuthClient(domain, email)
            val newUser = createUserWithProviderAccount(provider, providerUserId, email, oidcUser, organization.id, oauthClient.id)
            createOrganizationSetupProcess(newUser, oauthClient.id!!, organization.id!!)
            newUser
        }
    }

    private fun findSuperAdminEmail(merchantId: UUID): String =
        userRepository.findSuperAdminsByMerchantId(merchantId)
            .firstOrNull()?.email?.value
            ?: messageService.getMessage("oauth.error.admin_not_found")

    private fun generateSlug(domain: String): String {
        return domain.substringBefore(".")
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }

    private fun generateUniqueSlug(baseSlug: String): String {
        var slug = baseSlug
        var counter = 1
        while (organizationRepository.existsBySlug(slug)) {
            slug = "$baseSlug-$counter"
            counter++
        }
        return slug
    }

    private fun createOrganizationAndOAuthClient(domain: String, adminEmail: String): Pair<Organization, OAuthRegisteredClient> {
        val slug = generateUniqueSlug(generateSlug(domain))

        val organization = Organization(
            name = "-",
            slug = slug,
            status = OrganizationStatus.PENDING
        )
        organizationRepository.save(organization)
        log.info { "Created Organization via OIDC: ${organization.id} with slug: $slug" }

        val sandboxSecret = RandomStringUtils.secure().nextAlphanumeric(30)
        val productionSecret = RandomStringUtils.secure().nextAlphanumeric(30)

        val authMethodBasic = oauthClientConfigService.getAuthenticationMethod("client_secret_basic")
        val authMethodPost = oauthClientConfigService.getAuthenticationMethod("client_secret_post")
        val grantTypeCredentials = oauthClientConfigService.getGrantType("client_credentials")
        val grantTypeRefresh = oauthClientConfigService.getGrantType("refresh_token")
        val scopeOpenid = oauthClientConfigService.getScope("openid")
        val scopeProfile = oauthClientConfigService.getScope("profile")
        val scopeEmail = oauthClientConfigService.getScope("email")
        val scopeRead = oauthClientConfigService.getScope("read")
        val scopeWrite = oauthClientConfigService.getScope("write")

        val oauthClient = OAuthRegisteredClient().apply {
            id = organization.id!!
            clientId = UUID.randomUUID().toString()
            clientName = organization.name
            clientIdIssuedAt = Instant.now()
            sandboxClientSecret = passwordEncoder.encode(sandboxSecret)
            productionClientSecret = passwordEncoder.encode(productionSecret)
            this.domain = domain
            failedAuthAttempts = 0

            addAuthenticationMethod(authMethodBasic)
            addAuthenticationMethod(authMethodPost)
            addGrantType(grantTypeCredentials)
            addGrantType(grantTypeRefresh)
            addScope(scopeOpenid)
            addScope(scopeProfile)
            addScope(scopeEmail)
            addScope(scopeRead)
            addScope(scopeWrite)
            addSetting(OAuthClientSettingName.REQUIRE_AUTHORIZATION_CONSENT, "false")
            addSetting(OAuthClientSettingName.REQUIRE_PROOF_KEY, "false")
            addTokenSetting(OAuthTokenSettingName.ACCESS_TOKEN_TIME_TO_LIVE, "PT30M")
            addTokenSetting(OAuthTokenSettingName.REFRESH_TOKEN_TIME_TO_LIVE, "PT12H")
            addTokenSetting(OAuthTokenSettingName.REUSE_REFRESH_TOKENS, "false")
        }

        oauthClientRepository.save(oauthClient)
        log.info { "Created OAuth client via OIDC for organization: ${organization.name} with clientId: ${oauthClient.clientId}, domain: $domain" }

        organization.addProperty(OrganizationPropertyName.EMAIL, adminEmail)
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "false")
        organizationRepository.save(organization)

        return organization to oauthClient
    }

    private fun oauthError(errorCode: String, description: String): OAuth2AuthenticationException =
        OAuth2AuthenticationException(OAuth2Error(errorCode, description, null), description)

    private fun parseProvider(registrationId: String): OAuthProvider =
        when (registrationId.lowercase()) {
            "google" -> OAuthProvider.GOOGLE
            "microsoft" -> OAuthProvider.MICROSOFT
            else -> throw oauthError("unsupported_provider", messageService.getMessage("oauth.error.unsupported_provider", registrationId))
        }

    private fun findUserByProviderAccount(provider: OAuthProvider, providerUserId: String): OAuthUser? {
        return providerAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
            ?.also { account ->
                account.updateLastLogin()
                providerAccountRepository.save(account)
                log.info { "Existing OIDC user logged in: ${account.user.email?.value} via $provider" }
            }?.user
    }

    private fun findUserByEmailAndLinkProvider(email: String, provider: OAuthProvider, providerUserId: String): OAuthUser? {
        return userRepository.findByEmail(Email(email))?.also { user ->
            user.emailVerified = true
            user.registrationComplete = true
            userRepository.save(user)

            val providerAccount = OAuthProviderAccount(
                user = user,
                provider = provider,
                providerUserId = providerUserId,
                providerEmail = email
            )
            providerAccount.updateLastLogin()
            providerAccountRepository.save(providerAccount)
            log.info { "Linked $provider account to existing user: $email" }
        }
    }

    private fun createUserWithProviderAccount(
        provider: OAuthProvider,
        providerUserId: String,
        email: String,
        oidcUser: OidcUser,
        organizationId: UUID,
        oauthClientId: UUID
    ): OAuthUser {
        val firstName = oidcUser.givenName ?: oidcUser.fullName?.split(" ")?.firstOrNull()
        val lastName = oidcUser.familyName ?: oidcUser.fullName?.split(" ")?.drop(1)?.joinToString(" ")?.takeIf { it.isNotBlank() }
        val pictureUrl = oidcUser.picture

        val user = OAuthUser(
            username = email,
            password = passwordEncoder.encode(UUID.randomUUID().toString())!!,
            email = Email(email)
        ).apply {
            this.firstName = firstName
            this.lastName = lastName
            this.pictureUrl = pictureUrl
            this.emailVerified = true
            this.registrationComplete = true
            this.enabled = true
            this.organizationId = organizationId
            this.merchantId = oauthClientId
            this.userType = UserType.BUSINESS
            this.trustLevel = TrustLevel.TIER_THREE
            this.registrationSource = when (provider) {
                OAuthProvider.GOOGLE -> RegistrationSource.OAUTH_GOOGLE
                OAuthProvider.MICROSOFT -> RegistrationSource.OAUTH_MICROSOFT
            }
            this.authorities = mutableSetOf(
                "ROLE_MERCHANT_ADMIN",
                "ROLE_MERCHANT_SUPER_ADMIN",
                "ROLE_MERCHANT_USER"
            )
        }

        val savedUser = userRepository.save(user)

        val providerAccount = OAuthProviderAccount(
            user = savedUser,
            provider = provider,
            providerUserId = providerUserId,
            providerEmail = email
        )
        providerAccount.updateLastLogin()
        providerAccountRepository.save(providerAccount)

        log.info { "Created new super admin user via $provider OIDC: $email for organization: $organizationId" }
        return savedUser
    }

    private fun createOrganizationSetupProcess(user: OAuthUser, clientId: UUID, organizationId: UUID) {
        processGateway.createProcess(
            CreateNewProcessPayload(
                userId = user.id!!,
                publicId = UUID.randomUUID(),
                type = ProcessType.ORGANIZATION_SETUP,
                description = "Organization setup for ${user.email?.value}",
                initialState = ProcessState.PENDING,
                requestState = ProcessState.PENDING,
                channel = ProcessChannel.WEB_APP,
                data = mapOf(
                    ProcessRequestDataName.MERCHANT_ID to clientId.toString(),
                    ProcessRequestDataName.ORGANIZATION_ID to organizationId.toString(),
                    ProcessRequestDataName.USER_IDENTIFIER to user.id.toString()
                ),
                stakeholders = mapOf(
                    ProcessStakeholderType.FOR_USER to user.id.toString()
                )
            )
        )
        log.info { "Organization setup process created for OIDC user: ${user.id}" }
    }
}
