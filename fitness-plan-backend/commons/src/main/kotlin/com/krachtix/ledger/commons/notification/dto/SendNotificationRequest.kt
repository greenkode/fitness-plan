package com.krachtix.commons.notification.dto


import com.krachtix.commons.notification.enumeration.MessageChannel
import com.krachtix.commons.notification.enumeration.MessagePriority
import com.krachtix.commons.notification.enumeration.RecipientType
import com.krachtix.commons.notification.enumeration.TemplateName
import java.util.Locale

data class SendNotificationRequest(
    val recipients: List<MessageRecipient>,
    val templateName: TemplateName,
    val channel: MessageChannel,
    val priority: MessagePriority,
    val parameters: Map<String, String>,
    val locale: Locale,
    val clientIdentifier: String,
    val recipientType: RecipientType
)
