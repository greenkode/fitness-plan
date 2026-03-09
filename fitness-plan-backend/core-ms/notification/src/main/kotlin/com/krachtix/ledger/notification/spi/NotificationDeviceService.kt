package com.krachtix.notification.spi

import com.krachtix.commons.notification.CreateNotificationDevicePayload
import com.krachtix.commons.notification.NotificationDeviceGateway
import com.krachtix.commons.notification.dto.NotificationDeviceDto
import com.krachtix.commons.notification.enumeration.NotificationChannel
import com.krachtix.commons.notification.enumeration.NotificationType
import com.krachtix.notification.dao.NotificationDeviceEntity
import com.krachtix.notification.dao.NotificationDeviceJpaRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NotificationDeviceService(private val deviceRepository: NotificationDeviceJpaRepository) : NotificationDeviceGateway {

    override fun getDevice(
        notificationType: NotificationType,
        notificationChannel: NotificationChannel,
        userId: UUID
    ): NotificationDeviceDto? {

        return deviceRepository.findByUserIdAndNotificationChannelAndNotificationType(
            userId,
            notificationChannel,
            notificationType
        )?.toDomain()?.toDto()
    }

    override fun create(payload: CreateNotificationDevicePayload) {

        deviceRepository.save(NotificationDeviceEntity(payload.publicId, payload.notificationChannel, payload.value, payload.userId, payload.notificationType))
    }
}