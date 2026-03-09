package com.krachtix.notification.dao

import com.krachtix.commons.model.TenantAwareEntity
import com.krachtix.notification.domain.NotificationDevice
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import com.krachtix.commons.notification.enumeration.NotificationChannel
import com.krachtix.commons.notification.enumeration.NotificationType
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "notification_device")
class NotificationDeviceEntity(
    private val publicId: UUID,
    @Enumerated(STRING)
    private val notificationChannel: NotificationChannel,
    private val value: String,
    private val userId: UUID,
    @Enumerated(STRING)
    private val notificationType: NotificationType,
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private val id: Long? = null
) : TenantAwareEntity(), Serializable {


    fun toDomain(): NotificationDevice {
        return NotificationDevice(publicId, notificationChannel, value, userId, notificationType)
    }

    companion object {

        fun fromDomain(device: NotificationDevice): NotificationDeviceEntity {
            return NotificationDeviceEntity(
                device.publicId,
                device.notificationChannel,
                device.value,
                device.userId,
                device.notificationType
            )
        }
    }

}
