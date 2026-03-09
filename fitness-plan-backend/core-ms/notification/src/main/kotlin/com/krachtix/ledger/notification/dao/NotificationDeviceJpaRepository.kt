package com.krachtix.notification.dao

import com.krachtix.commons.notification.enumeration.NotificationChannel
import com.krachtix.commons.notification.enumeration.NotificationType
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface NotificationDeviceJpaRepository : CrudRepository<NotificationDeviceEntity, Long> {

    fun findByUserIdAndNotificationChannelAndNotificationType(
        userId: UUID,
        notificationChannel: NotificationChannel,
        notificationType: NotificationType
    ): NotificationDeviceEntity?
}