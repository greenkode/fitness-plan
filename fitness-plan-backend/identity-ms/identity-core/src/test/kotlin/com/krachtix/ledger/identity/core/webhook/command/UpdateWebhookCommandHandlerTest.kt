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
class UpdateWebhookCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var webhookConfigService: WebhookConfigService

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: UpdateWebhookCommandHandler

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
            this.merchantId = this@UpdateWebhookCommandHandlerTest.merchantId
        }
    }

    @Nested
    @DisplayName("Successful Webhook Update")
    inner class SuccessfulUpdate {

        @Test
        fun `should update webhook and return result`() {
            val webhookPublicId = UUID.randomUUID()
            val command = UpdateWebhookCommand(
                publicId = webhookPublicId,
                name = "Updated Webhook",
                url = "https://example.com/updated",
                eventTypes = listOf("transaction.created"),
                description = "Updated description"
            )

            val updatedEntity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "Updated Webhook"
                url = "https://example.com/updated"
                status = WebhookConfigStatus.ACTIVE
                description = "Updated description"
                setEventTypeList(listOf("transaction.created"))
            }

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.updateWebhook(
                publicId = webhookPublicId,
                clientId = merchantId,
                name = "Updated Webhook",
                url = "https://example.com/updated",
                eventTypes = listOf("transaction.created"),
                description = "Updated description"
            )).thenReturn(updatedEntity)

            val result = handler.handle(command)

            assertThat(result.publicId).isEqualTo(webhookPublicId)
            assertThat(result.name).isEqualTo("Updated Webhook")
            assertThat(result.url).isEqualTo("https://example.com/updated")
            assertThat(result.eventTypes).containsExactly("transaction.created")
            assertThat(result.description).isEqualTo("Updated description")
            assertThat(result.status).isEqualTo(WebhookConfigStatus.ACTIVE)
        }

        @Test
        fun `should create process before updating webhook`() {
            val webhookPublicId = UUID.randomUUID()
            val command = UpdateWebhookCommand(
                publicId = webhookPublicId,
                name = "Updated Webhook",
                url = null,
                eventTypes = null,
                description = null
            )

            val updatedEntity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "Updated Webhook"
                url = "https://example.com/original"
                status = WebhookConfigStatus.ACTIVE
                setEventTypeList(listOf("transaction.created"))
            }

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.updateWebhook(
                publicId = webhookPublicId,
                clientId = merchantId,
                name = "Updated Webhook",
                url = null,
                eventTypes = null,
                description = null
            )).thenReturn(updatedEntity)

            handler.handle(command)

            verify(processGateway).createProcess(capture(processPayloadCaptor))

            val payload = processPayloadCaptor.value
            assertThat(payload.userId).isEqualTo(userId)
            assertThat(payload.type).isEqualTo(ProcessType.WEBHOOK_UPDATE)
        }

        @Test
        fun `should complete process after successful update`() {
            val webhookPublicId = UUID.randomUUID()
            val command = UpdateWebhookCommand(
                publicId = webhookPublicId,
                name = "Updated Webhook",
                url = null,
                eventTypes = null,
                description = null
            )

            val updatedEntity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "Updated Webhook"
                url = "https://example.com/original"
                status = WebhookConfigStatus.ACTIVE
                setEventTypeList(listOf("transaction.created"))
            }

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.updateWebhook(
                publicId = webhookPublicId,
                clientId = merchantId,
                name = "Updated Webhook",
                url = null,
                eventTypes = null,
                description = null
            )).thenReturn(updatedEntity)

            handler.handle(command)

            verify(processGateway).completeProcess(any(), any())
        }

        @Test
        fun `should handle partial update with only name`() {
            val webhookPublicId = UUID.randomUUID()
            val command = UpdateWebhookCommand(
                publicId = webhookPublicId,
                name = "New Name Only",
                url = null,
                eventTypes = null,
                description = null
            )

            val updatedEntity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "New Name Only"
                url = "https://example.com/original"
                status = WebhookConfigStatus.ACTIVE
                setEventTypeList(listOf("transaction.created"))
            }

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.updateWebhook(
                publicId = webhookPublicId,
                clientId = merchantId,
                name = "New Name Only",
                url = null,
                eventTypes = null,
                description = null
            )).thenReturn(updatedEntity)

            val result = handler.handle(command)

            assertThat(result.name).isEqualTo("New Name Only")
            assertThat(result.url).isEqualTo("https://example.com/original")
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

            val command = UpdateWebhookCommand(
                publicId = UUID.randomUUID(),
                name = "Updated Webhook",
                url = null,
                eventTypes = null,
                description = null
            )

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Merchant not found")

            verify(webhookConfigService, never()).updateWebhook(
                publicId = any(),
                clientId = any(),
                name = any(),
                url = any(),
                eventTypes = any(),
                description = any()
            )
            verify(processGateway, never()).createProcess(any())
        }
    }

    @Nested
    @DisplayName("Webhook Not Found")
    inner class WebhookNotFound {

        @Test
        fun `should propagate exception when webhook not found`() {
            val webhookPublicId = UUID.randomUUID()
            val command = UpdateWebhookCommand(
                publicId = webhookPublicId,
                name = "Updated Webhook",
                url = null,
                eventTypes = null,
                description = null
            )

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.updateWebhook(
                publicId = webhookPublicId,
                clientId = merchantId,
                name = "Updated Webhook",
                url = null,
                eventTypes = null,
                description = null
            )).thenThrow(RecordNotFoundException("Webhook not found"))

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Webhook not found")
        }
    }
}
