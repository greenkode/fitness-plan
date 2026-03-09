package com.krachtix.user.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfiguration.ALL
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration(
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val coreJwtAuthenticationConverter: CoreJwtAuthenticationConverter,
    @Value("\${spring.security.permit-all-paths:}")
    private val permitAllPaths: Array<String>,
    @Value("\${application.security.allowed-origins}")
    private val allowedOrigins: Array<String>,
) {

    @Bean
    fun resourceServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests { authorize ->
            authorize.requestMatchers(*permitAllPaths).permitAll()
            authorize.anyRequest().authenticated()
        }

        http.oauth2ResourceServer { resourceServerConfigurer ->
            resourceServerConfigurer.jwt { jwt ->
                jwt.jwtAuthenticationConverter(coreJwtAuthenticationConverter)
            }.authenticationEntryPoint(customAuthenticationEntryPoint)
        }

        http.cors(Customizer.withDefaults()).csrf { it.disable() }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.addAllowedHeader(ALL)
        config.addAllowedMethod(ALL)
        allowedOrigins.forEach { config.addAllowedOrigin(it) }
        config.allowCredentials = true
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
