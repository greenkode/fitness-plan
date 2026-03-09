package com.krachtix.notification.service

import com.krachtix.commons.notification.NotificationChannelHandler
import com.krachtix.commons.notification.dto.MessagePayload
import com.krachtix.commons.notification.dto.MessageRecipient
import com.krachtix.commons.notification.enumeration.MessageChannel
import com.krachtix.commons.notification.enumeration.MessagePriority
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationRouterTest {

    private val webhookHandler = mock<NotificationChannelHandler>()
    private val emailHandler = mock<NotificationChannelHandler>()

    private fun createPayload(channel: MessageChannel) = MessagePayload(
        recipient = MessageRecipient(address = UUID.randomUUID().toString()),
        channel = channel,
        priority = MessagePriority.NORMAL,
        parameters = mapOf("event_type" to "TRANSACTION_CREATED"),
        clientIdentifier = UUID.randomUUID().toString()
    )

    @Nested
    @DisplayName("Channel Routing")
    inner class ChannelRouting {

        @Test
        fun `should route to correct handler based on channel`() {
            whenever(webhookHandler.channel).thenReturn(MessageChannel.WEBHOOK)
            whenever(emailHandler.channel).thenReturn(MessageChannel.EMAIL)

            val router = NotificationRouter(listOf(webhookHandler, emailHandler))
            val payload = createPayload(MessageChannel.WEBHOOK)

            router.publish(payload)

            verify(webhookHandler).handle(payload)
            verify(emailHandler, never()).handle(payload)
        }

        @Test
        fun `should route email to email handler`() {
            whenever(webhookHandler.channel).thenReturn(MessageChannel.WEBHOOK)
            whenever(emailHandler.channel).thenReturn(MessageChannel.EMAIL)

            val router = NotificationRouter(listOf(webhookHandler, emailHandler))
            val payload = createPayload(MessageChannel.EMAIL)

            router.publish(payload)

            verify(emailHandler).handle(payload)
            verify(webhookHandler, never()).handle(payload)
        }

        @Test
        fun `should not throw for unregistered channel`() {
            whenever(webhookHandler.channel).thenReturn(MessageChannel.WEBHOOK)

            val router = NotificationRouter(listOf(webhookHandler))
            val payload = createPayload(MessageChannel.SMS)

            router.publish(payload)

            verify(webhookHandler, never()).handle(payload)
        }
    }
}
