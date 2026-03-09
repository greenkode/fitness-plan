package com.krachtix.commons.notification

import com.krachtix.commons.notification.enumeration.NotificationChannel
import com.krachtix.commons.notification.dto.NotificationDeviceDto
import com.krachtix.commons.notification.enumeration.NotificationType
import java.util.UUID

interface NotificationDeviceGateway {

    fun getDevice(
        notificationType: NotificationType,
        notificationChannel: NotificationChannel,
        userId: UUID
    ): NotificationDeviceDto?

    fun create(payload: CreateNotificationDevicePayload)
}