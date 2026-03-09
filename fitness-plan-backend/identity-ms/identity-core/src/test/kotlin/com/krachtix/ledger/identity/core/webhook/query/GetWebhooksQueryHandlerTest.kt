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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetWebhooksQueryHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var webhookConfigService: WebhookConfigService

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: GetWebhooksQueryHandler

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
            this.merchantId = this@GetWebhooksQueryHandlerTest.merchantId
        }
    }

    @Nested
    @DisplayName("Successful Webhook Listing")
    inner class SuccessfulListing {

        @Test
        fun `should return page of webhooks`() {
            val query = GetWebhooksQuery(page = 0, size = 10)

            val entity1 = WebhookConfigEntity().apply {
                publicId = UUID.randomUUID()
                name = "Webhook 1"
                url = "https://example.com/webhook1"
                status = WebhookConfigStatus.ACTIVE
                description = "First webhook"
                setEventTypeList(listOf("transaction.created"))
                createdAt = Instant.now()
            }

            val entity2 = WebhookConfigEntity().apply {
                publicId = UUID.randomUUID()
                name = "Webhook 2"
                url = "https://example.com/webhook2"
                status = WebhookConfigStatus.DISABLED
                description = null
                setEventTypeList(listOf("account.updated", "transaction.created"))
                createdAt = Instant.now()
            }

            val page = PageImpl(listOf(entity1, entity2), PageRequest.of(0, 10), 2)

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.listWebhooks(merchantId, 0, 10)).thenReturn(page)

            val result = handler.handle(query)

            assertThat(result.webhooks.content).hasSize(2)
            assertThat(result.webhooks.content[0].name).isEqualTo("Webhook 1")
            assertThat(result.webhooks.content[0].url).isEqualTo("https://example.com/webhook1")
            assertThat(result.webhooks.content[0].status).isEqualTo(WebhookConfigStatus.ACTIVE)
            assertThat(result.webhooks.content[1].name).isEqualTo("Webhook 2")
            assertThat(result.webhooks.content[1].status).isEqualTo(WebhookConfigStatus.DISABLED)
        }

        @Test
        fun `should return empty page when no webhooks exist`() {
            val query = GetWebhooksQuery(page = 0, size = 10)

            val emptyPage = PageImpl<WebhookConfigEntity>(emptyList(), PageRequest.of(0, 10), 0)

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.listWebhooks(merchantId, 0, 10)).thenReturn(emptyPage)

            val result = handler.handle(query)

            assertThat(result.webhooks.content).isEmpty()
            assertThat(result.webhooks.totalElements).isEqualTo(0)
        }

        @Test
        fun `should map entity event types to response`() {
            val query = GetWebhooksQuery(page = 0, size = 10)

            val entity = WebhookConfigEntity().apply {
                publicId = UUID.randomUUID()
                name = "Webhook"
                url = "https://example.com/webhook"
                status = WebhookConfigStatus.ACTIVE
                setEventTypeList(listOf("transaction.created", "account.updated", "payment.completed"))
                createdAt = Instant.now()
            }

            val page = PageImpl(listOf(entity), PageRequest.of(0, 10), 1)

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.listWebhooks(merchantId, 0, 10)).thenReturn(page)

            val result = handler.handle(query)

            assertThat(result.webhooks.content[0].eventTypes)
                .containsExactly("transaction.created", "account.updated", "payment.completed")
        }

        @Test
        fun `should pass correct pagination parameters`() {
            val query = GetWebhooksQuery(page = 2, size = 5)

            val emptyPage = PageImpl<WebhookConfigEntity>(emptyList(), PageRequest.of(2, 5), 0)

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.listWebhooks(merchantId, 2, 5)).thenReturn(emptyPage)

            handler.handle(query)

            verify(webhookConfigService).listWebhooks(merchantId, 2, 5)
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

            val query = GetWebhooksQuery(page = 0, size = 10)

            assertThatThrownBy { handler.handle(query) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Merchant not found")

            verify(webhookConfigService, never()).listWebhooks(any(), any(), any())
        }
    }
}
