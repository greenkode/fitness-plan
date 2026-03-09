package com.krachtix.notification.domain.command

import an.awesome.pipelinr.Command
import an.awesome.pipelinr.Voidy
import com.krachtix.commons.notification.enumeration.NotificationType
import java.net.URL

data class RegisterWebhookCommand(val notificationType: NotificationType, val url: URL) : Command<Voidy>