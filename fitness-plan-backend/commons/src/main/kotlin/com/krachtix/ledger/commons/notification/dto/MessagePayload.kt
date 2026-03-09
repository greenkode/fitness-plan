package com.krachtix.commons.notification.dto

import com.krachtix.commons.notification.enumeration.MessageChannel
import com.krachtix.commons.notification.enumeration.MessagePriority


data class MessagePayload(
    val recipient: MessageRecipient,
    val template: MessageTemplateDto? = null,
    val channel: MessageChannel,
    val priority: MessagePriority,
    val parameters: Map<String, Any>,
    val clientIdentifier: String,
    val content: String? = null
)