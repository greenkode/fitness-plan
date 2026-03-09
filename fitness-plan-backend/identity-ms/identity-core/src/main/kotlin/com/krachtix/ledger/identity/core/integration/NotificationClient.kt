package com.krachtix.identity.core.integration

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.api.ApiResponse
import com.krachtix.commons.notification.dto.MessageRecipient
import com.krachtix.commons.notification.dto.SendNotificationRequest
import com.krachtix.commons.notification.dto.SendNotificationResponse
import com.krachtix.commons.notification.enumeration.MessageChannel
import com.krachtix.commons.notification.enumeration.MessagePriority
import com.krachtix.commons.notification.enumeration.RecipientType
import com.krachtix.commons.notification.enumeration.TemplateName
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import tools.jackson.databind.json.JsonMapper
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.Locale

private val log = KotlinLogging.logger {}

@Service
class NotificationClient(
    @param:Value("\${notification-ms.base-url}")
    private val notificationServiceUrl: String,
    private val authorizedClientManager: OAuth2AuthorizedClientManager,
    @param:Value("\${spring.security.oauth2.client.registration.identity-ms-client.client-id}")
    private val clientRegistrationId: String,
    objectMapper: JsonMapper
) {
    private val restClient = RestClient.builder()
        .baseUrl(notificationServiceUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .configureMessageConverters { it.registerDefaults().withJsonConverter(JacksonJsonHttpMessageConverter(objectMapper)) }
        .build()

    private fun getAccessToken(): String? {
        val authorizeRequest = OAuth2AuthorizeRequest
            .withClientRegistrationId("identity-ms-client")
            .principal(clientRegistrationId)
            .build()

        val authorizedClient = authorizedClientManager.authorize(authorizeRequest)
        return authorizedClient?.accessToken?.tokenValue
    }

    fun sendNotification(
        recipients: List<MessageRecipient>,
        templateName: TemplateName,
        channel: MessageChannel = MessageChannel.EMAIL,
        priority: MessagePriority = MessagePriority.HIGH,
        parameters: Map<String, String>,
        locale: Locale = Locale.ENGLISH,
        clientIdentifier: String,
        recipientType: RecipientType = RecipientType.INDIVIDUAL
    ): SendNotificationResponse? {
        log.info { "Sending notification via notification-ms: template=$templateName" }

        val request = SendNotificationRequest(
            recipients = recipients,
            templateName = templateName,
            channel = channel,
            priority = priority,
            parameters = parameters,
            locale = locale,
            clientIdentifier = clientIdentifier,
            recipientType = recipientType
        )

        val accessToken = getAccessToken()
        if (accessToken == null) {
            log.warn { "Failed to obtain access token for notification-ms" }
            return null
        }

        return runCatching {
            val response = restClient.post()
                .uri("/api/v1/notifications/send")
                .headers { it.setBearerAuth(accessToken) }
                .body(request)
                .retrieve()
                .body(object : ParameterizedTypeReference<ApiResponse<SendNotificationResponse>>() {})

            log.info { "Notification sent successfully: ${response?.data}" }
            response?.data
        }.getOrElse { e ->
            log.error(e) { "Failed to send notification via notification-ms" }
            null
        }
    }
}
