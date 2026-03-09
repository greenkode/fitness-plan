package com.krachtix.identity.core.webhook.query

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.WebhookConfigEntity
import com.krachtix.identity.core.entity.WebhookConfigStatus
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.service.WebhookConfigService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetWebhookQueryHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var webhookConfigService: WebhookConfigService

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: GetWebhookQueryHandler

    private val merchantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private lateinit var user: OAuthUser

    @BeforeEach
    fun setUp() {
        user = OAuthUser(
            username = "test@example.com",
            password = "encoded",
            email = Email("test@example.com")
        ).apply {
            id = userId
            this.merchantId = this@GetWebhookQueryHandlerTest.merchantId
        }
    }

    @Nested
    @DisplayName("Successful Webhook Retrieval")
    inner class SuccessfulRetrieval {

        @Test
        fun `should return webhook response for valid public id`() {
            val webhookPublicId = UUID.randomUUID()
            val query = GetWebhookQuery(publicId = webhookPublicId)
            val createdAt = Instant.now()

            val entity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "My Webhook"
                url = "https://example.com/webhook"
                status = WebhookConfigStatus.ACTIVE
                description = "Test description"
                setEventTypeList(listOf("transaction.created", "account.updated"))
                this.createdAt = createdAt
            }

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.getWebhook(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenReturn(entity)

            val result = handler.handle(query)

            assertThat(result.webhook.publicId).isEqualTo(webhookPublicId)
            assertThat(result.webhook.name).isEqualTo("My Webhook")
            assertThat(result.webhook.url).isEqualTo("https://example.com/webhook")
            assertThat(result.webhook.status).isEqualTo(WebhookConfigStatus.ACTIVE)
            assertThat(result.webhook.description).isEqualTo("Test description")
            assertThat(result.webhook.eventTypes).containsExactly("transaction.created", "account.updated")
            assertThat(result.webhook.createdAt).isEqualTo(createdAt)
        }

        @Test
        fun `should return webhook with null description`() {
            val webhookPublicId = UUID.randomUUID()
            val query = GetWebhookQuery(publicId = webhookPublicId)

            val entity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "My Webhook"
                url = "https://example.com/webhook"
                status = WebhookConfigStatus.ACTIVE
                description = null
                setEventTypeList(listOf("transaction.created"))
                createdAt = Instant.now()
            }

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.getWebhook(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenReturn(entity)

            val result = handler.handle(query)

            assertThat(result.webhook.description).isNull()
        }

        @Test
        fun `should call service with correct merchant id from user context`() {
            val webhookPublicId = UUID.randomUUID()
            val query = GetWebhookQuery(publicId = webhookPublicId)

            val entity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "My Webhook"
                url = "https://example.com/webhook"
                status = WebhookConfigStatus.ACTIVE
                setEventTypeList(listOf("transaction.created"))
                createdAt = Instant.now()
            }

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.getWebhook(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenReturn(entity)

            handler.handle(query)

            verify(webhookConfigService).getWebhook(
                publicId = webhookPublicId,
                clientId = merchantId
            )
        }
    }

    @Nested
    @DisplayName("Merchant Not Found")
    inner class MerchantNotFound {

        @Test
        fun `should throw when user has no merchant id`() {
            val userWithoutMerchant = OAuthUser(
                username = "test@example.com",
                password = "encoded",
                email = Email("test@example.com")
            ).apply {
                id = userId
                merchantId = null
            }

            whenever(userService.getCurrentUser()).thenReturn(userWithoutMerchant)
            whenever(messageService.getMessage("webhook.error.merchant_not_found")).thenReturn("Merchant not found")

            val query = GetWebhookQuery(publicId = UUID.randomUUID())

            assertThatThrownBy { handler.handle(query) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Merchant not found")

            verify(webhookConfigService, never()).getWebhook(publicId = any(), clientId = any())
        }
    }

    @Nested
    @DisplayName("Webhook Not Found")
    inner class WebhookNotFound {

        @Test
        fun `should propagate exception when webhook not found`() {
            val webhookPublicId = UUID.randomUUID()
            val query = GetWebhookQuery(publicId = webhookPublicId)

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.getWebhook(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenThrow(RecordNotFoundException("Webhook not found"))

            assertThatThrownBy { handler.handle(query) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Webhook not found")
        }
    }
}
