package com.krachtix.notification.api.request

import com.krachtix.commons.notification.enumeration.NotificationType

import com.krachtix.notification.domain.command.RegisterWebhookCommand
import java.net.URL

data class RegisterWebhookRequest(val notificationType: NotificationType, val url: URL) {

    fun toCommand(): RegisterWebhookCommand {
        return RegisterWebhookCommand(notificationType, url)
    }

}