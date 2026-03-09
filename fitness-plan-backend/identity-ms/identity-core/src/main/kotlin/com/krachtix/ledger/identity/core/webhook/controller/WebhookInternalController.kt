package com.krachtix.identity.core.webhook.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.core.service.WebhookConfigService
import com.krachtix.identity.core.webhook.dto.ActiveWebhookDto
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/internal/webhooks")
@PreAuthorize("hasAuthority('SCOPE_internal')")
class WebhookInternalController(
    private val webhookConfigService: WebhookConfigService
) {

    @GetMapping("/active")
    fun getActiveWebhooks(
        @RequestParam merchantId: UUID,
        @RequestParam eventType: String
    ): List<ActiveWebhookDto> {
        log.info { "Fetching active webhooks for merchant: $merchantId, eventType: $eventType" }
        return webhookConfigService.getActiveWebhookDtosForEvent(merchantId, eventType)
    }

    @GetMapping("/{publicId}")
    fun getWebhookByPublicId(@PathVariable publicId: UUID): ActiveWebhookDto? {
        log.info { "Fetching webhook config by publicId: $publicId" }
        return webhookConfigService.getWebhookDtoByPublicId(publicId)
    }
}
