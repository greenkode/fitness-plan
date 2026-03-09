package com.krachtix.identity.commons.notification

import java.util.Locale

data class NotificationRequest(
    val recipients: List<MessageRecipientRequest>,
    val templateName: String,
    val channel: String,
    val priority: String,
    val parameters: Map<String, String>,
    val locale: Locale,
    val clientIdentifier: String,
    val recipientType: String
)

data class MessageRecipientRequest(
    val address: String,
    val name: String? = null
)

fun MessagePayload.toNotificationRequest(): NotificationRequest {
    return NotificationRequest(
        recipients = this.recipients.map { MessageRecipientRequest(it.address, it.name) },
        templateName = this.templateName,
        channel = this.channel,
        priority = this.priority,
        parameters = this.parameters,
        locale = this.locale,
        clientIdentifier = this.clientIdentifier,
        recipientType = this.recipientType
    )
}