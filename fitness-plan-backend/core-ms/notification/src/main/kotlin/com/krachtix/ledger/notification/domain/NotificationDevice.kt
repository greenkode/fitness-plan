package com.krachtix.notification.domain

import com.krachtix.commons.notification.dto.NotificationDeviceDto
import com.krachtix.commons.notification.enumeration.NotificationChannel
import com.krachtix.commons.notification.enumeration.NotificationType
import java.util.UUID


data class NotificationDevice(
    val publicId: UUID,
    val notificationChannel: NotificationChannel,
    val value: String,
    val userId: UUID,
    val notificationType: NotificationType,
) {

    fun toDto(): NotificationDeviceDto {
        return NotificationDeviceDto(publicId, notificationChannel, value, userId, notificationType)
    }

}
