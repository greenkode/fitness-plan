package com.krachtix.identity.core.webhook.controller

import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.security.IsMerchantSuperAdmin
import com.krachtix.identity.core.webhook.command.CreateWebhookCommand
import com.krachtix.identity.core.webhook.command.DeleteWebhookCommand
import com.krachtix.identity.core.webhook.command.RotateWebhookSecretCommand
import com.krachtix.identity.core.webhook.command.TestWebhookCommand
import com.krachtix.identity.core.webhook.command.UpdateWebhookCommand
import com.krachtix.identity.core.webhook.dto.CreateWebhookRequest
import com.krachtix.identity.core.webhook.dto.CreateWebhookResponse
import com.krachtix.identity.core.webhook.dto.RotateWebhookSecretResponse
import com.krachtix.identity.core.webhook.dto.TestWebhookResponse
import com.krachtix.identity.core.webhook.dto.UpdateWebhookRequest
import com.krachtix.identity.core.webhook.dto.WebhookListResponse
import com.krachtix.identity.core.webhook.dto.WebhookResponse
import com.krachtix.identity.core.webhook.query.GetWebhookQuery
import com.krachtix.identity.core.webhook.query.GetWebhooksQuery
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/merchant/webhooks")
@IsMerchantSuperAdmin
class WebhookController(
    private val pipeline: Pipeline
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createWebhook(@RequestBody request: CreateWebhookRequest): CreateWebhookResponse {
        log.info { "Creating webhook: ${request.name}" }

        val result = pipeline.send(
            CreateWebhookCommand(
                name = request.name,
                url = request.url,
                eventTypes = request.eventTypes,
                description = request.description
            )
        )

        return CreateWebhookResponse(
            publicId = result.publicId,
            name = result.name,
            url = result.url,
            signingSecret = result.signingSecret,
            eventTypes = result.eventTypes,
            description = result.description,
            status = result.status
        )
    }

    @GetMapping
    fun listWebhooks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): WebhookListResponse {
        log.info { "Listing webhooks, page: $page, size: $size" }

        val result = pipeline.send(GetWebhooksQuery(page = page, size = size))

        return WebhookListResponse(
            webhooks = result.webhooks.content,
            page = result.webhooks.number,
            size = result.webhooks.size,
            totalElements = result.webhooks.totalElements,
            totalPages = result.webhooks.totalPages
        )
    }

    @GetMapping("/{publicId}")
    fun getWebhook(@PathVariable publicId: UUID): WebhookResponse {
        log.info { "Getting webhook: $publicId" }

        val result = pipeline.send(GetWebhookQuery(publicId = publicId))

        return result.webhook
    }

    @PutMapping("/{publicId}")
    fun updateWebhook(
        @PathVariable publicId: UUID,
        @RequestBody request: UpdateWebhookRequest
    ): WebhookResponse {
        log.info { "Updating webhook: $publicId" }

        val result = pipeline.send(
            UpdateWebhookCommand(
                publicId = publicId,
                name = request.name,
                url = request.url,
                eventTypes = request.eventTypes,
                description = request.description
            )
        )

        return WebhookResponse(
            publicId = result.publicId,
            name = result.name,
            url = result.url,
            eventTypes = result.eventTypes,
            description = result.description,
            status = result.status,
            createdAt = null
        )
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteWebhook(@PathVariable publicId: UUID) {
        log.info { "Deleting webhook: $publicId" }

        pipeline.send(DeleteWebhookCommand(publicId = publicId))
    }

    @PostMapping("/{publicId}/rotate-secret")
    fun rotateSecret(@PathVariable publicId: UUID): RotateWebhookSecretResponse {
        log.info { "Rotating secret for webhook: $publicId" }

        val result = pipeline.send(RotateWebhookSecretCommand(publicId = publicId))

        return RotateWebhookSecretResponse(
            publicId = result.publicId,
            newSecret = result.newSecret
        )
    }

    @PostMapping("/{publicId}/test")
    fun testWebhook(@PathVariable publicId: UUID): TestWebhookResponse {
        log.info { "Testing webhook: $publicId" }

        val result = pipeline.send(TestWebhookCommand(publicId = publicId))

        return TestWebhookResponse(
            success = result.success,
            statusCode = result.statusCode,
            errorMessage = result.errorMessage
        )
    }
}
