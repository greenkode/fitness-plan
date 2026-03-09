package com.krachtix.commons.notification.dto

import com.krachtix.commons.notification.enumeration.DeliveryStatus

data class SendNotificationResponse(
    val status: DeliveryStatus,
    val messageId: String,
    val message: String? = null
)
