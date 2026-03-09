package com.krachtix.commons.notification.dto

import com.krachtix.commons.notification.enumeration.MessageChannel
import com.krachtix.commons.notification.enumeration.RecipientType
import com.krachtix.commons.notification.enumeration.TemplateName
import java.util.Locale

data class MessageTemplateDto(val channel: MessageChannel,
                              val content: String,
                              val title: String,
                              val name: TemplateName,
                              val locale: Locale,
                              val externalId: String,
                              val active: Boolean,
                              val recipientType: RecipientType
)