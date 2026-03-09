package com.krachtix.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SwaggerSecurityConfiguration(
    @Value("\${springdoc.security.username:swagger}")
    private val swaggerUsername: String,
    @Value("\${springdoc.security.password:}")
    private val swaggerPassword: String,
    @Value("\${springdoc.security.enabled:true}")
    private val securityEnabled: Boolean
) {

    @Bean
    @Order(1)
    fun swaggerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")

        if (securityEnabled && swaggerPassword.isNotBlank()) {
            http
                .authorizeHttpRequests { it.anyRequest().authenticated() }
                .httpBasic(Customizer.withDefaults())
        } else {
            http.authorizeHttpRequests { it.anyRequest().permitAll() }
        }

        http.csrf { it.disable() }

        return http.build()
    }

    @Bean
    fun swaggerUserDetailsService(): UserDetailsService {
        if (!securityEnabled || swaggerPassword.isBlank()) {
            return InMemoryUserDetailsManager()
        }

        val user = User.builder()
            .username(swaggerUsername)
            .password("{noop}$swaggerPassword")
            .roles("SWAGGER")
            .build()

        return InMemoryUserDetailsManager(user)
    }
}
