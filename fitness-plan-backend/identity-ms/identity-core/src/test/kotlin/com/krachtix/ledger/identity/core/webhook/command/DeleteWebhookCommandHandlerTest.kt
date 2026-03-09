package com.krachtix.identity.core.webhook.command

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.entity.OAuthUser
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
class DeleteWebhookCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var webhookConfigService: WebhookConfigService

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: DeleteWebhookCommandHandler

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
            this.merchantId = this@DeleteWebhookCommandHandlerTest.merchantId
        }
    }

    @Nested
    @DisplayName("Successful Webhook Deletion")
    inner class SuccessfulDeletion {

        @Test
        fun `should delete webhook and return success`() {
            val webhookPublicId = UUID.randomUUID()
            val command = DeleteWebhookCommand(publicId = webhookPublicId)

            whenever(userService.getCurrentUser()).thenReturn(user)

            val result = handler.handle(command)

            assertThat(result.success).isTrue()
            verify(webhookConfigService).deleteWebhook(publicId = webhookPublicId, clientId = merchantId)
        }

        @Test
        fun `should create process before deleting webhook`() {
            val webhookPublicId = UUID.randomUUID()
            val command = DeleteWebhookCommand(publicId = webhookPublicId)

            whenever(userService.getCurrentUser()).thenReturn(user)

            handler.handle(command)

            verify(processGateway).createProcess(capture(processPayloadCaptor))

            val payload = processPayloadCaptor.value
            assertThat(payload.userId).isEqualTo(userId)
            assertThat(payload.type).isEqualTo(ProcessType.WEBHOOK_DELETION)
        }

        @Test
        fun `should complete process after successful deletion`() {
            val webhookPublicId = UUID.randomUUID()
            val command = DeleteWebhookCommand(publicId = webhookPublicId)

            whenever(userService.getCurrentUser()).thenReturn(user)

            handler.handle(command)

            verify(processGateway).completeProcess(any(), any())
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

            val command = DeleteWebhookCommand(publicId = UUID.randomUUID())

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Merchant not found")

            verify(webhookConfigService, never()).deleteWebhook(publicId = any(), clientId = any())
            verify(processGateway, never()).createProcess(any())
        }
    }

    @Nested
    @DisplayName("Webhook Not Found")
    inner class WebhookNotFound {

        @Test
        fun `should propagate exception when webhook not found`() {
            val webhookPublicId = UUID.randomUUID()
            val command = DeleteWebhookCommand(publicId = webhookPublicId)

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.deleteWebhook(publicId = webhookPublicId, clientId = merchantId))
                .thenThrow(RecordNotFoundException("Webhook not found"))

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Webhook not found")
        }
    }
}
