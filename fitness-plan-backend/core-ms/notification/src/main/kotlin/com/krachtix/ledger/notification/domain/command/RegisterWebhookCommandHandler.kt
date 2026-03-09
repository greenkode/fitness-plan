package com.krachtix.notification.domain.command

import an.awesome.pipelinr.Command
import an.awesome.pipelinr.Voidy
import com.krachtix.commons.exception.DuplicateRecordException
import com.krachtix.commons.notification.enumeration.NotificationChannel
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.notification.CreateNotificationDevicePayload
import com.krachtix.commons.notification.NotificationDeviceGateway
import com.krachtix.commons.user.UserGateway
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RegisterWebhookCommandHandler(
    private val gateway: NotificationDeviceGateway,
    private val userGateway: UserGateway,
    private val messageService: MessageService
) : Command.Handler<RegisterWebhookCommand, Voidy> {

    override fun handle(command: RegisterWebhookCommand): Voidy {
        val loggedInUserId = userGateway.getLoggedInUserId()!!

        gateway.getDevice(command.notificationType, NotificationChannel.WEBHOOK, loggedInUserId)
            ?.let {
                throw DuplicateRecordException(
                    messageService.getMessage(
                        "webhook.already.exists.for.notification.type.0",
                        command.notificationType
                    )
                )
            }

        gateway.create(
            CreateNotificationDevicePayload(
                UUID.randomUUID(),
                NotificationChannel.WEBHOOK,
                command.url.toString(),
                loggedInUserId,
                command.notificationType
            )
        )

        return Voidy()
    }
}