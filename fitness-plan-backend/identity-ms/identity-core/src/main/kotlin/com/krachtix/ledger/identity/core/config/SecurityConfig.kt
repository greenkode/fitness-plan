package com.krachtix.identity.core.config

import com.krachtix.identity.core.oauth.CustomOidcUserService
import com.krachtix.identity.core.oauth.OAuth2AuthenticationFailureHandler
import com.krachtix.identity.core.oauth.OAuth2AuthenticationSuccessHandler
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationEventPublisher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    @Lazy private val clientLockoutAuthenticationProvider: ClientLockoutAuthenticationProvider,
    private val customOAuth2ErrorResponseHandler: CustomOAuth2ErrorResponseHandler,
    private val clientLockoutFilter: ClientLockoutFilter,
    private val jwtAuthenticationConverter: CustomJwtAuthenticationConverter,
    private val publicEndpointBearerTokenResolver: PublicEndpointBearerTokenResolver,
    @Lazy private val customOidcUserService: CustomOidcUserService,
    @Lazy private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    @Lazy private val oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler,
    @Lazy private val jwtDecoder: JwtDecoder,
    @org.springframework.beans.factory.annotation.Value("\${jwt.issuer:http://localhost:9083}") private val jwtIssuer: String
) {
    @Bean
    @Order(1)
    @Throws(Exception::class)
    fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain? {
        http
            .oauth2AuthorizationServer { authorizationServer ->
                http.securityMatcher(authorizationServer.endpointsMatcher)
                authorizationServer
                    .oidc(Customizer.withDefaults())
                    .clientAuthentication { clientAuth ->
                        clientAuth
                            .authenticationProviders { providers ->
                                providers.add(0, clientLockoutAuthenticationProvider)
                            }
                            .errorResponseHandler(customOAuth2ErrorResponseHandler)
                    }
            }
            .cors(Customizer.withDefaults())
            .addFilterBefore(clientLockoutFilter, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests { authorize ->
                authorize
                    .anyRequest().authenticated()
            }
            .exceptionHandling { exceptions: ExceptionHandlingConfigurer<HttpSecurity?>? ->
                exceptions!!
                    .defaultAuthenticationEntryPointFor(
                        LoginUrlAuthenticationEntryPoint("/login"),
                        MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                    )
            }

        return http.build()
    }

    @Bean
    @Order(2)
    @Throws(Exception::class)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain? {
        http
            .cors(Customizer.withDefaults())
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/test/**", "/login", "/api/login", "/auth/2fa/**", "/merchant/invitation/validate", "/merchant/invitation/complete", "/password-reset/**", "/auth/registration/**", "/v3/api-docs/**", "/swagger-ui/**", "/api/webhooks/**")
            }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(
                        "/oauth2/**",
                        "/.well-known/**",
                        "/error",
                        "/test/**",
                        "/",
                        "/api/login",
                        "/auth/2fa/login",
                        "/auth/2fa/refresh",
                        "/auth/2fa/logout",
                        "/actuator/**",
                        "/merchant/invitation/validate",
                        "/merchant/invitation/complete",
                        "/password-reset/**",
                        "/auth/registration/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/login/oauth2/code/**",
                        "/oauth2/authorization/**",
                        "/api/webhooks/**"
                    ).permitAll()
                    .requestMatchers("/auth/2fa/verify", "/auth/2fa/resend")
                        .hasAnyAuthority("ROLE_TWO_FACTOR_AUTH", "ROLE_MERCHANT_USER")
                    .requestMatchers("/api/**", "/internal/**").authenticated()
                    .anyRequest().hasAuthority("ROLE_MERCHANT_USER")
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2
                    .bearerTokenResolver(publicEndpointBearerTokenResolver)
                    .jwt { jwt ->
                        jwt.decoder(jwtDecoder)
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                    }
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .authorizationEndpoint { it.baseUri("/oauth2/authorization") }
                    .redirectionEndpoint { it.baseUri("/login/oauth2/code/*") }
                    .userInfoEndpoint {
                        it.oidcUserService(customOidcUserService)
                    }
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(oAuth2AuthenticationFailureHandler)
            }
            .formLogin { form ->
                form
                    .defaultSuccessUrl("/login-success", true)
                    .permitAll()
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }


    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings {
        return AuthorizationServerSettings.builder()
            .issuer(jwtIssuer)
            .build()
    }

    @Bean
    fun authenticationProvider(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): DaoAuthenticationProvider {
        val authProvider = DaoAuthenticationProvider(userDetailsService)
        authProvider.setPasswordEncoder(passwordEncoder)
        return authProvider
    }

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        return authConfig.authenticationManager
    }

    @Bean
    fun authenticationEventPublisher(applicationEventPublisher: ApplicationEventPublisher): AuthenticationEventPublisher {
        return DefaultAuthenticationEventPublisher(applicationEventPublisher)
    }

    @Bean
    fun tokenGenerator(
        jwtTokenCustomizer: OAuth2TokenCustomizer<JwtEncodingContext>,
        jwkSource: JWKSource<SecurityContext>
    ): OAuth2TokenGenerator<*> {
        val jwtEncoder: JwtEncoder = NimbusJwtEncoder(jwkSource)
        val jwtGenerator = JwtGenerator(jwtEncoder)
        jwtGenerator.setJwtCustomizer(jwtTokenCustomizer)

        val accessTokenGenerator = OAuth2AccessTokenGenerator()
        val refreshTokenGenerator = OAuth2RefreshTokenGenerator()

        return DelegatingOAuth2TokenGenerator(
            jwtGenerator,
            accessTokenGenerator,
            refreshTokenGenerator
        )
    }

}