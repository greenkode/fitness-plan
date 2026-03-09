package com.krachtix.notification.service

import com.krachtix.commons.tenant.TenantContext
import com.krachtix.commons.tenant.TenantContextData
import com.krachtix.commons.webhook.WebhookConfigDto
import com.krachtix.commons.webhook.WebhookConfigGateway
import com.krachtix.commons.webhook.WebhookDeliveryStatus
import com.krachtix.commons.webhook.WebhookEventType
import com.krachtix.notification.dao.WebhookDeliveryEntity
import com.krachtix.notification.dao.WebhookDeliveryRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class WebhookDeliveryServiceTest {

    @Mock
    private lateinit var webhookDeliveryRepository: WebhookDeliveryRepository

    @Mock
    private lateinit var webhookConfigGateway: WebhookConfigGateway

    @Mock
    private lateinit var restClient: RestClient

    private lateinit var service: WebhookDeliveryService

    private val merchantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = WebhookDeliveryService(
            webhookDeliveryRepository = webhookDeliveryRepository,
            webhookConfigGateway = webhookConfigGateway,
            restClient = restClient
        )
        TenantContext.setContext(TenantContextData(merchantId))
    }

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    @Nested
    @DisplayName("Get Delivery History")
    inner class GetDeliveryHistory {

        @Test
        fun `should return paginated delivery history for current merchant`() {
            val entity = WebhookDeliveryEntity(
                webhookPublicId = UUID.randomUUID(),
                eventType = WebhookEventType.TRANSACTION_CREATED,
                eventId = UUID.randomUUID(),
                payload = """{"key":"value"}""",
                status = WebhookDeliveryStatus.SUCCESS,
                attemptCount = 1,
                lastAttemptAt = Instant.now(),
                lastStatusCode = 200
            ).apply {
                this.merchantId = this@WebhookDeliveryServiceTest.merchantId
                this.createdAt = Instant.now()
            }

            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(listOf(entity), pageable, 1)

            whenever(webhookDeliveryRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable))
                .thenReturn(page)

            val result = service.getDeliveryHistory(pageable)

            assertEquals(1, result.totalElements)
            assertEquals(entity.webhookPublicId, result.content[0].webhookPublicId)
            assertEquals(WebhookDeliveryStatus.SUCCESS, result.content[0].status)
            assertEquals(200, result.content[0].lastStatusCode)
        }

        @Test
        fun `should return empty page when no deliveries exist`() {
            val pageable = PageRequest.of(0, 20)
            val emptyPage = PageImpl<WebhookDeliveryEntity>(emptyList(), pageable, 0)

            whenever(webhookDeliveryRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable))
                .thenReturn(emptyPage)

            val result = service.getDeliveryHistory(pageable)

            assertTrue(result.content.isEmpty())
            assertEquals(0, result.totalElements)
        }
    }

    @Nested
    @DisplayName("Process Delivery Batch")
    inner class ProcessDeliveryBatch {

        @Test
        fun `should fetch pending retries and process them`() {
            val retryStatuses = listOf(WebhookDeliveryStatus.PENDING, WebhookDeliveryStatus.RETRYING)

            whenever(
                webhookDeliveryRepository.findPendingRetries(
                    argThat<List<WebhookDeliveryStatus>> { containsAll(retryStatuses) },
                    any(),
                    any()
                )
            ).thenReturn(emptyList())

            service.processDeliveryBatch(50)

            verify(webhookDeliveryRepository).findPendingRetries(
                argThat<List<WebhookDeliveryStatus>> { containsAll(retryStatuses) },
                any(),
                any()
            )
        }

        @Test
        fun `should not save when no deliveries to process`() {
            whenever(webhookDeliveryRepository.findPendingRetries(any(), any(), any()))
                .thenReturn(emptyList())

            service.processDeliveryBatch(50)

            verify(webhookDeliveryRepository, never()).save(any<WebhookDeliveryEntity>())
        }

        @Test
        fun `should mark delivery as failed when webhook config no longer available`() {
            val delivery = WebhookDeliveryEntity(
                webhookPublicId = UUID.randomUUID(),
                eventType = WebhookEventType.TRANSACTION_CREATED,
                eventId = UUID.randomUUID(),
                payload = """{"key":"value"}""",
                status = WebhookDeliveryStatus.PENDING,
                nextRetryAt = Instant.now().minusSeconds(10)
            ).apply { this.merchantId = this@WebhookDeliveryServiceTest.merchantId }

            whenever(webhookDeliveryRepository.findPendingRetries(any(), any(), any()))
                .thenReturn(listOf(delivery))
            whenever(webhookConfigGateway.getWebhookByPublicId(delivery.webhookPublicId))
                .thenReturn(null)
            whenever(webhookDeliveryRepository.save(any<WebhookDeliveryEntity>()))
                .thenAnswer { it.arguments[0] }

            service.processDeliveryBatch(50)

            verify(webhookDeliveryRepository).save(argThat<WebhookDeliveryEntity> {
                status == WebhookDeliveryStatus.FAILED &&
                    lastError == "Webhook configuration no longer available"
            })
        }
    }
}
