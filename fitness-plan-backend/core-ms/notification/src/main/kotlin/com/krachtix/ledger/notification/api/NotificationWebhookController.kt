package com.krachtix.notification.api

import an.awesome.pipelinr.Pipeline
import com.krachtix.commons.security.IsMerchantAdmin
import com.krachtix.notification.api.request.RegisterWebhookRequest
import org.springframework.http.HttpStatus.CREATED
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@IsMerchantAdmin
@RestController
@RequestMapping("/notifications/webhooks")
class NotificationWebhookController(private val pipeline: Pipeline) {

    @PostMapping("/register")
    @ResponseStatus(CREATED)
    fun registerWebhook(@RequestBody request: RegisterWebhookRequest) {
        pipeline.send(request.toCommand())
    }
}