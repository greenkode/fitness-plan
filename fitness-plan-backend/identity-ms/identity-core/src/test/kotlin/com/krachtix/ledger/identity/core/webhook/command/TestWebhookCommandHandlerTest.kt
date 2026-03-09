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
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.net.URI
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TestWebhookCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var webhookConfigService: WebhookConfigService

    @Mock
    private lateinit var messageService: MessageService

    @Mock
    private lateinit var restClient: RestClient

    @Mock
    private lateinit var requestBodyUriSpec: RestClient.RequestBodyUriSpec

    @Mock
    private lateinit var requestBodySpec: RestClient.RequestBodySpec

    @Mock
    private lateinit var responseSpec: RestClient.ResponseSpec

    @InjectMocks
    private lateinit var handler: TestWebhookCommandHandler

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
            this.merchantId = this@TestWebhookCommandHandlerTest.merchantId
        }
    }

    @Nested
    @DisplayName("Successful Webhook Test")
    inner class SuccessfulTest {

        @Test
        fun `should return success when webhook endpoint responds with 2xx`() {
            val webhookPublicId = UUID.randomUUID()
            val command = TestWebhookCommand(publicId = webhookPublicId)

            val entity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "My Webhook"
                url = "https://example.com/webhook"
                secretHash = "encrypted-secret"
                status = WebhookConfigStatus.ACTIVE
            }

            val responseEntity = ResponseEntity.ok().build<Void>()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.getWebhook(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenReturn(entity)
            whenever(webhookConfigService.decryptSecret("encrypted-secret")).thenReturn("decrypted-secret")
            whenever(restClient.post()).thenReturn(requestBodyUriSpec)
            whenever(requestBodyUriSpec.uri("https://example.com/webhook")).thenReturn(requestBodySpec)
            whenever(requestBodySpec.contentType(any())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.body(any<String>())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.retrieve()).thenReturn(responseSpec)
            whenever(responseSpec.toBodilessEntity()).thenReturn(responseEntity)

            val result = handler.handle(command)

            assertThat(result.success).isTrue()
            assertThat(result.statusCode).isEqualTo(200)
            assertThat(result.errorMessage).isNull()
        }

        @Test
        fun `should return failure when webhook endpoint responds with non-2xx`() {
            val webhookPublicId = UUID.randomUUID()
            val command = TestWebhookCommand(publicId = webhookPublicId)

            val entity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "My Webhook"
                url = "https://example.com/webhook"
                secretHash = "encrypted-secret"
                status = WebhookConfigStatus.ACTIVE
            }

            val responseEntity = ResponseEntity.status(HttpStatusCode.valueOf(500))
                .build<Void>()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.getWebhook(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenReturn(entity)
            whenever(webhookConfigService.decryptSecret("encrypted-secret")).thenReturn("decrypted-secret")
            whenever(restClient.post()).thenReturn(requestBodyUriSpec)
            whenever(requestBodyUriSpec.uri("https://example.com/webhook")).thenReturn(requestBodySpec)
            whenever(requestBodySpec.contentType(any())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.body(any<String>())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.retrieve()).thenReturn(responseSpec)
            whenever(responseSpec.toBodilessEntity()).thenReturn(responseEntity)

            val result = handler.handle(command)

            assertThat(result.success).isFalse()
            assertThat(result.statusCode).isEqualTo(500)
            assertThat(result.errorMessage).isNull()
        }

        @Test
        fun `should include X-Webhook-Signature header in request`() {
            val webhookPublicId = UUID.randomUUID()
            val command = TestWebhookCommand(publicId = webhookPublicId)

            val entity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "My Webhook"
                url = "https://example.com/webhook"
                secretHash = "encrypted-secret"
                status = WebhookConfigStatus.ACTIVE
            }

            val responseEntity = ResponseEntity.ok().build<Void>()

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.getWebhook(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenReturn(entity)
            whenever(webhookConfigService.decryptSecret("encrypted-secret")).thenReturn("decrypted-secret")
            whenever(restClient.post()).thenReturn(requestBodyUriSpec)
            whenever(requestBodyUriSpec.uri("https://example.com/webhook")).thenReturn(requestBodySpec)
            whenever(requestBodySpec.contentType(any())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.body(any<String>())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.retrieve()).thenReturn(responseSpec)
            whenever(responseSpec.toBodilessEntity()).thenReturn(responseEntity)

            handler.handle(command)

            verify(requestBodySpec).header(org.mockito.kotlin.eq("X-Webhook-Signature"), any())
        }
    }

    @Nested
    @DisplayName("Failed Webhook Test")
    inner class FailedTest {

        @Test
        fun `should return failure when rest client throws exception`() {
            val webhookPublicId = UUID.randomUUID()
            val command = TestWebhookCommand(publicId = webhookPublicId)

            val entity = WebhookConfigEntity().apply {
                publicId = webhookPublicId
                name = "My Webhook"
                url = "https://example.com/webhook"
                secretHash = "encrypted-secret"
                status = WebhookConfigStatus.ACTIVE
            }

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.getWebhook(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenReturn(entity)
            whenever(webhookConfigService.decryptSecret("encrypted-secret")).thenReturn("decrypted-secret")
            whenever(restClient.post()).thenReturn(requestBodyUriSpec)
            whenever(requestBodyUriSpec.uri("https://example.com/webhook")).thenReturn(requestBodySpec)
            whenever(requestBodySpec.contentType(any())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.body(any<String>())).thenReturn(requestBodySpec)
            whenever(requestBodySpec.retrieve()).thenThrow(RestClientException("Connection refused"))
            whenever(messageService.getMessage("webhook.error.test_failed")).thenReturn("Webhook test failed")

            val result = handler.handle(command)

            assertThat(result.success).isFalse()
            assertThat(result.statusCode).isNull()
            assertThat(result.errorMessage).isEqualTo("Webhook test failed")
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

            val command = TestWebhookCommand(publicId = UUID.randomUUID())

            assertThatThrownBy { handler.handle(command) }
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
            val command = TestWebhookCommand(publicId = webhookPublicId)

            whenever(userService.getCurrentUser()).thenReturn(user)
            whenever(webhookConfigService.getWebhook(
                publicId = webhookPublicId,
                clientId = merchantId
            )).thenThrow(RecordNotFoundException("Webhook not found"))

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("Webhook not found")
        }
    }
}
