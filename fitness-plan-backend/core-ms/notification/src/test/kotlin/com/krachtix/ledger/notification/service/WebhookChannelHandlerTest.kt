package com.krachtix.notification.service

import com.krachtix.commons.notification.dto.MessagePayload
import com.krachtix.commons.notification.dto.MessageRecipient
import com.krachtix.commons.notification.enumeration.MessageChannel
import com.krachtix.commons.notification.enumeration.MessagePriority
import com.krachtix.commons.webhook.WebhookConfigDto
import com.krachtix.commons.webhook.WebhookConfigGateway
import com.krachtix.commons.webhook.WebhookEventType
import com.krachtix.notification.dao.WebhookDeliveryEntity
import com.krachtix.notification.dao.WebhookDeliveryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class WebhookChannelHandlerTest {

    @Mock
    private lateinit var webhookDeliveryRepository: WebhookDeliveryRepository

    @Mock
    private lateinit var webhookConfigGateway: WebhookConfigGateway

    private val objectMapper = JsonMapper.builder().build()

    private lateinit var handler: WebhookChannelHandler

    private val merchantId = UUID.randomUUID()
    private val webhookPublicId1 = UUID.randomUUID()
    private val webhookPublicId2 = UUID.randomUUID()
    private val eventId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        handler = WebhookChannelHandler(webhookDeliveryRepository, webhookConfigGateway, objectMapper)
    }

    private fun createPayload(
        eventType: String = WebhookEventType.TRANSACTION_CREATED.name,
        params: Map<String, Any> = mapOf("reference" to "TXN-001")
    ) = MessagePayload(
        recipient = MessageRecipient(address = merchantId.toString()),
        channel = MessageChannel.WEBHOOK,
        priority = MessagePriority.NORMAL,
        parameters = params + ("event_type" to eventType),
        clientIdentifier = eventId.toString()
    )

    private fun createWebhookConfig(publicId: UUID) = WebhookConfigDto(
        publicId = publicId,
        url = "https://example.com/webhook",
        signingSecret = "test-secret",
        eventTypes = listOf(WebhookEventType.TRANSACTION_CREATED.name)
    )

    @Nested
    @DisplayName("Delivery Record Creation")
    inner class DeliveryRecordCreation {

        @Test
        fun `should create delivery records for each active webhook config`() {
            val configs = listOf(createWebhookConfig(webhookPublicId1), createWebhookConfig(webhookPublicId2))
            whenever(webhookConfigGateway.getActiveWebhooks(merchantId, WebhookEventType.TRANSACTION_CREATED.name))
                .thenReturn(configs)
            whenever(webhookDeliveryRepository.save(any<WebhookDeliveryEntity>()))
                .thenAnswer { it.arguments[0] }

            handler.handle(createPayload())

            verify(webhookDeliveryRepository).save(argThat<WebhookDeliveryEntity> {
                webhookPublicId == webhookPublicId1
            })
            verify(webhookDeliveryRepository).save(argThat<WebhookDeliveryEntity> {
                webhookPublicId == webhookPublicId2
            })
        }

        @Test
        fun `should set nextRetryAt on new deliveries`() {
            val configs = listOf(createWebhookConfig(webhookPublicId1))
            whenever(webhookConfigGateway.getActiveWebhooks(merchantId, WebhookEventType.TRANSACTION_CREATED.name))
                .thenReturn(configs)
            whenever(webhookDeliveryRepository.save(any<WebhookDeliveryEntity>()))
                .thenAnswer { it.arguments[0] }

            handler.handle(createPayload())

            verify(webhookDeliveryRepository).save(argThat<WebhookDeliveryEntity> {
                nextRetryAt != null
            })
        }

        @Test
        fun `should set correct merchantId on delivery entity`() {
            val configs = listOf(createWebhookConfig(webhookPublicId1))
            whenever(webhookConfigGateway.getActiveWebhooks(merchantId, WebhookEventType.TRANSACTION_CREATED.name))
                .thenReturn(configs)
            whenever(webhookDeliveryRepository.save(any<WebhookDeliveryEntity>()))
                .thenAnswer { it.arguments[0] }

            handler.handle(createPayload())

            verify(webhookDeliveryRepository).save(argThat<WebhookDeliveryEntity> {
                this.merchantId == this@WebhookChannelHandlerTest.merchantId
            })
        }

        @Test
        fun `should set correct event type and event id`() {
            val configs = listOf(createWebhookConfig(webhookPublicId1))
            whenever(webhookConfigGateway.getActiveWebhooks(merchantId, WebhookEventType.TRANSACTION_CREATED.name))
                .thenReturn(configs)
            whenever(webhookDeliveryRepository.save(any<WebhookDeliveryEntity>()))
                .thenAnswer { it.arguments[0] }

            handler.handle(createPayload())

            verify(webhookDeliveryRepository).save(argThat<WebhookDeliveryEntity> {
                eventType == WebhookEventType.TRANSACTION_CREATED && eventId == this@WebhookChannelHandlerTest.eventId
            })
        }
    }

    @Nested
    @DisplayName("Envelope Structure")
    inner class EnvelopeStructure {

        @Test
        fun `should build correct envelope with id, type, timestamp, version, and data`() {
            val configs = listOf(createWebhookConfig(webhookPublicId1))
            whenever(webhookConfigGateway.getActiveWebhooks(merchantId, WebhookEventType.TRANSACTION_CREATED.name))
                .thenReturn(configs)
            whenever(webhookDeliveryRepository.save(any<WebhookDeliveryEntity>()))
                .thenAnswer { it.arguments[0] }

            handler.handle(createPayload())

            verify(webhookDeliveryRepository).save(argThat<WebhookDeliveryEntity> {
                val parsed = objectMapper.readValue(payload, Map::class.java)
                parsed["id"] == eventId.toString()
                    && parsed["type"] == WebhookEventType.TRANSACTION_CREATED.name
                    && parsed["version"] == "1.0"
                    && parsed["timestamp"] != null
                    && (parsed["data"] as Map<*, *>).containsKey("reference")
                    && !(parsed["data"] as Map<*, *>).containsKey("event_type")
            })
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `should not save records when no active webhooks exist`() {
            whenever(webhookConfigGateway.getActiveWebhooks(merchantId, WebhookEventType.TRANSACTION_CREATED.name))
                .thenReturn(emptyList())

            handler.handle(createPayload())

            verify(webhookDeliveryRepository, never()).save(any<WebhookDeliveryEntity>())
        }

        @Test
        fun `should not save records when event_type is missing`() {
            val payload = MessagePayload(
                recipient = MessageRecipient(address = merchantId.toString()),
                channel = MessageChannel.WEBHOOK,
                priority = MessagePriority.NORMAL,
                parameters = mapOf("reference" to "TXN-001"),
                clientIdentifier = eventId.toString()
            )

            handler.handle(payload)

            verify(webhookDeliveryRepository, never()).save(any<WebhookDeliveryEntity>())
        }

        @Test
        fun `should report channel as WEBHOOK`() {
            assertEquals(MessageChannel.WEBHOOK, handler.channel)
        }
    }
}
