package com.krachtix.commons.notification.dto

interface NotificationPublisher {
    fun publish(payload: MessagePayload)
}
