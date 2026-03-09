package com.krachtix.identity.core.webhook.command

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
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
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CreateWebhookCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var webhookConfigService: WebhookConfigService

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: CreateWebhookCommandHandler

    @Captor
    private lateinit var processPayloadCaptor: ArgumentCaptor<CreateNewProcessPayload>

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
            this.merchantId = this@CreateWebhookCommandHandlerTest.merchantId
        }
    }

    @Nested
    @DisplayName("Successful Webhook Creation")
    inner class SuccessfulCreation {

        @Test
        fun `should create webhook and return result with signing secret`() {
            val command = CreateWebhookCommand(
                name = "My Webhook",
                url = "https://example.com/webhook",
                eventTypes = listOf("transaction.created", "account.updated"),
                description = "Test webhook"
            )

            val entity = WebhookConfigEntity().apply {
                publicId = UUID.randomUUID()
                name = "My Webhook"
                url = "https://example.com/webhook"
                status = WebhookConfigStatus.ACTIVE
                description = "Test webhook"
                setEventTypeList(listOf("transaction.created", "account.updated"))
            }

            val secret = "generated-secret-12345678901234".toCharArray()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.createWebhook(
                clientId = merchantId,
                name = "My Webhook",
                url = "https://example.com/webhook",
                eventTypes = listOf("transaction.created", "account.updated"),
                description = "Test webhook"
            )).thenReturn(entity to secret)

            val result = handler.handle(command)

            assertThat(result.publicId).isEqualTo(entity.publicId)
            assertThat(result.name).isEqualTo("My Webhook")
            assertThat(result.url).isEqualTo("https://example.com/webhook")
            assertThat(result.signingSecret).isNotBlank()
            assertThat(result.eventTypes).containsExactly("transaction.created", "account.updated")
            assertThat(result.description).isEqualTo("Test webhook")
            assertThat(result.status).isEqualTo(WebhookConfigStatus.ACTIVE)
        }

        @Test
        fun `should create process before creating webhook`() {
            val command = CreateWebhookCommand(
                name = "My Webhook",
                url = "https://example.com/webhook",
                eventTypes = listOf("transaction.created"),
                description = null
            )

            val entity = WebhookConfigEntity().apply {
                publicId = UUID.randomUUID()
                name = "My Webhook"
                url = "https://example.com/webhook"
                status = WebhookConfigStatus.ACTIVE
                setEventTypeList(listOf("transaction.created"))
            }

            val secret = "generated-secret-12345678901234".toCharArray()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.createWebhook(
                clientId = merchantId,
                name = "My Webhook",
                url = "https://example.com/webhook",
                eventTypes = listOf("transaction.created"),
                description = null
            )).thenReturn(entity to secret)

            handler.handle(command)

            verify(processGateway).createProcess(capture(processPayloadCaptor))

            val payload = processPayloadCaptor.value
            assertThat(payload.userId).isEqualTo(userId)
            assertThat(payload.type).isEqualTo(ProcessType.WEBHOOK_CREATION)
        }

        @Test
        fun `should complete process after successful creation`() {
            val command = CreateWebhookCommand(
                name = "My Webhook",
                url = "https://example.com/webhook",
                eventTypes = listOf("transaction.created"),
                description = null
            )

            val entity = WebhookConfigEntity().apply {
                publicId = UUID.randomUUID()
                name = "My Webhook"
                url = "https://example.com/webhook"
                status = WebhookConfigStatus.ACTIVE
                setEventTypeList(listOf("transaction.created"))
            }

            val secret = "generated-secret-12345678901234".toCharArray()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.createWebhook(
                clientId = merchantId,
                name = "My Webhook",
                url = "https://example.com/webhook",
                eventTypes = listOf("transaction.created"),
                description = null
            )).thenReturn(entity to secret)

            handler.handle(command)

            verify(processGateway).completeProcess(any(), any())
        }

        @Test
        fun `should zero secret char array after use`() {
            val command = CreateWebhookCommand(
                name = "My Webhook",
                url = "https://example.com/webhook",
                eventTypes = listOf("transaction.created"),
                description = null
            )

            val entity = WebhookConfigEntity().apply {
                publicId = UUID.randomUUID()
                name = "My Webhook"
                url = "https://example.com/webhook"
                status = WebhookConfigStatus.ACTIVE
                setEventTypeList(listOf("transaction.created"))
            }

            val secret = "generated-secret-12345678901234".toCharArray()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.createWebhook(
                clientId = merchantId,
                name = "My Webhook",
                url = "https://example.com/webhook",
                eventTypes = listOf("transaction.created"),
                description = null
            )).thenReturn(entity to secret)

            handler.handle(command)

            assertThat(secret).containsOnly('\u0000')
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

            val command = CreateWebhookCommand(
                name = "My Webhook",
                url = "https://example.com/webhook",
                eventTypes = listOf("transaction.created"),
                description = null
            )

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Merchant not found")

            verify(webhookConfigService, never()).createWebhook(
                clientId = any(),
                name = any(),
                url = any(),
                eventTypes = any(),
                description = any()
            )
            verify(processGateway, never()).createProcess(any())
        }
    }
}
