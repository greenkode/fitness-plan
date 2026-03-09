package com.krachtix.commons.notification

import com.krachtix.commons.notification.dto.MessagePayload
import com.krachtix.commons.notification.enumeration.MessageChannel

interface NotificationChannelHandler {
    val channel: MessageChannel
    fun handle(payload: MessagePayload)
}
