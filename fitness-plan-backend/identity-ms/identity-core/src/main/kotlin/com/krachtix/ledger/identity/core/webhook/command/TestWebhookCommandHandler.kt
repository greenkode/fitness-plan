package com.krachtix.identity.core.webhook.command

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.service.WebhookConfigService
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class TestWebhookCommandHandler(
    private val userService: UserService,
    private val webhookConfigService: WebhookConfigService,
    private val messageService: MessageService,
    private val restClient: RestClient
) : Command.Handler<TestWebhookCommand, TestWebhookResult> {

    override fun handle(command: TestWebhookCommand): TestWebhookResult {
        log.info { "Processing TestWebhookCommand for publicId: ${command.publicId}" }

        val user = userService.getCurrentUser()
        val merchantId = user.merchantId
            ?: throw RecordNotFoundException(messageService.getMessage("webhook.error.merchant_not_found"))

        val entity = webhookConfigService.getWebhook(
            publicId = command.publicId,
            clientId = merchantId
        )

        val payload = """{"event":"webhook.test","timestamp":"${Instant.now()}"}"""
        val secret = webhookConfigService.decryptSecret(entity.secretHash)
        val signature = computeHmacSha256(payload, secret)

        return runCatching {
            val response = restClient.post()
                .uri(entity.url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Webhook-Signature", signature)
                .body(payload)
                .retrieve()
                .toBodilessEntity()

            val statusCode = response.statusCode.value()
            log.info { "Webhook test completed with status: $statusCode for publicId: ${command.publicId}" }

            TestWebhookResult(
                success = response.statusCode.is2xxSuccessful,
                statusCode = statusCode,
                errorMessage = null
            )
        }.getOrElse { ex ->
            log.warn(ex) { "Webhook test failed for publicId: ${command.publicId}" }

            TestWebhookResult(
                success = false,
                statusCode = null,
                errorMessage = messageService.getMessage("webhook.error.test_failed")
            )
        }
    }

    private fun computeHmacSha256(data: String, secret: String): String {
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
