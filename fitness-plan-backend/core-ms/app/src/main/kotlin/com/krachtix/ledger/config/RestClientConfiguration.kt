package com.krachtix.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Duration

@Configuration
class RestClientConfiguration {

    @Bean
    fun restClient(): RestClient {
        val mapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .addModule(KotlinModule.Builder().build())
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(Duration.ofSeconds(5))
        factory.setReadTimeout(Duration.ofSeconds(30))

        return RestClient.builder()
            .requestFactory(factory)
            .configureMessageConverters { it.registerDefaults().withJsonConverter(JacksonJsonHttpMessageConverter(mapper)) }
            .build()
    }
}
