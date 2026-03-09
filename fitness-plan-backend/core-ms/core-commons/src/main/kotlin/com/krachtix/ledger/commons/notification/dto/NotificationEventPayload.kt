package com.krachtix.commons.notification.dto

import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.notification.enumeration.NotificationChannel
import com.krachtix.commons.notification.enumeration.NotificationType
import com.krachtix.notification.dto.NotificationRecipient
import com.krachtix.notification.enumeration.NotificationParameter
import com.krachtix.notification.enumeration.NotificationPriority
import com.krachtix.notification.enumeration.TemplateName
import java.io.Serializable
import java.util.Locale
import java.util.UUID


data class NotificationEventPayload(
    val recipients: List<NotificationRecipient>,
    val templateName: TemplateName,
    val channel: NotificationChannel,
    val priority: NotificationPriority,
    val type: NotificationType,
    val parameters: Map<NotificationParameter, String>,
    val locale: Locale,
    val userId: String? = null,
    val processId: UUID? = null,
    val accountType: String? = null
) : Serializable {

    fun requiredParameterValue(parameter: NotificationParameter): String {
        return this.parameters[parameter] ?: throw RecordNotFoundException(parameter.name)
    }
}
