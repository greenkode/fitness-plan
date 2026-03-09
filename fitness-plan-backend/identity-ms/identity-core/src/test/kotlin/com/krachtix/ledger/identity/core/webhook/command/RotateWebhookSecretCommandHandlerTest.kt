package com.krachtix.identity.core.webhook.command

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
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RotateWebhookSecretCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var webhookConfigService: WebhookConfigService

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: RotateWebhookSecretCommandHandler

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
            this.merchantId = this@RotateWebhookSecretCommandHandlerTest.merchantId
        }
    }

    @Nested
    @DisplayName("Successful Secret Rotation")
    inner class SuccessfulRotation {

        @Test
        fun `should rotate secret and return new secret`() {
            val webhookPublicId = UUID.randomUUID()
            val command = RotateWebhookSecretCommand(publicId = webhookPublicId)

            val entity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "My Webhook"
                url = "https://example.com/webhook"
                status = WebhookConfigStatus.ACTIVE
            }

            val newSecret = "new-rotated-secret-123456789012".toCharArray()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.rotateSecret(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenReturn(entity to newSecret)

            val result = handler.handle(command)

            assertThat(result.publicId).isEqualTo(webhookPublicId)
            assertThat(result.newSecret).isNotBlank()
        }

        @Test
        fun `should zero secret char array after use`() {
            val webhookPublicId = UUID.randomUUID()
            val command = RotateWebhookSecretCommand(publicId = webhookPublicId)

            val entity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "My Webhook"
                url = "https://example.com/webhook"
                status = WebhookConfigStatus.ACTIVE
            }

            val newSecret = "new-rotated-secret-123456789012".toCharArray()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.rotateSecret(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenReturn(entity to newSecret)

            handler.handle(command)

            assertThat(newSecret).containsOnly('\u0000')
        }

        @Test
        fun `should call rotate secret on service with correct parameters`() {
            val webhookPublicId = UUID.randomUUID()
            val command = RotateWebhookSecretCommand(publicId = webhookPublicId)

            val entity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "My Webhook"
                url = "https://example.com/webhook"
                status = WebhookConfigStatus.ACTIVE
            }

            val newSecret = "new-rotated-secret-123456789012".toCharArray()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.rotateSecret(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenReturn(entity to newSecret)

            handler.handle(command)

            verify(webhookConfigService).rotateSecret(
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

            val command = RotateWebhookSecretCommand(publicId = UUID.randomUUID())

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Merchant not found")

            verify(webhookConfigService, never()).rotateSecret(publicId = any(), clientId = any())
        }
    }

    @Nested
    @DisplayName("Webhook Not Found")
    inner class WebhookNotFound {

        @Test
        fun `should propagate exception when webhook not found`() {
            val webhookPublicId = UUID.randomUUID()
            val command = RotateWebhookSecretCommand(publicId = webhookPublicId)

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.rotateSecret(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenThrow(RecordNotFoundException("Webhook not found"))

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Webhook not found")
        }
    }
}
