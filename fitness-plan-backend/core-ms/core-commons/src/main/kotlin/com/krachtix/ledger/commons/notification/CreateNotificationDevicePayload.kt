package com.krachtix.commons.notification

import com.krachtix.commons.notification.enumeration.NotificationChannel
import com.krachtix.commons.notification.enumeration.NotificationType
import java.util.UUID


data class CreateNotificationDevicePayload(
    val publicId: UUID,
    val notificationChannel: NotificationChannel,
    val value: String,
    val userId: UUID,
    val notificationType: NotificationType,
)
