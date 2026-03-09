package com.krachtix.commons.webhook

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.webhook.WebhookResponse
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.net.URL


@Component
class WebhookSender(private val objectMapper: JsonMapper) {

    private val log = KotlinLogging.logger {}

    fun send(url: URL, payload: WebhookResponse) {
        try {
            RestClient.builder()
                .configureMessageConverters { it.registerDefaults().withJsonConverter(JacksonJsonHttpMessageConverter(objectMapper)) }
                .build()
                .post()
                .uri(url.toString())
                .body(payload)
                .retrieve()
        } catch (ex: Exception) {
            log.error(ex) { "Error while sending webhook to url $url" }
        }
    }

}

