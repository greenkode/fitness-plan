package com.krachtix.notification.service

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.notification.NotificationChannelHandler
import com.krachtix.commons.notification.dto.MessagePayload
import com.krachtix.commons.notification.dto.NotificationPublisher
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class NotificationRouter(
    handlers: List<NotificationChannelHandler>
) : NotificationPublisher {

    private val handlerMap = handlers.associateBy { it.channel }

    override fun publish(payload: MessagePayload) {
        val handler = handlerMap[payload.channel]
            ?: run {
                log.warn { "No handler registered for channel: ${payload.channel}" }
                return
            }
        handler.handle(payload)
    }
}
