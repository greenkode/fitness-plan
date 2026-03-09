package com.krachtix.identity.core.settings.command

import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.MakeProcessRequestPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class SavePreferencesCommandHandler(
    private val userService: UserService,
    private val processGateway: ProcessGateway,
    private val messageService: MessageService
) : Command.Handler<SavePreferencesCommand, SavePreferencesResult> {

    override fun handle(command: SavePreferencesCommand): SavePreferencesResult {
        val user = userService.getCurrentUser()

        user.merchantId
            ?: throw RecordNotFoundException(messageService.getMessage("settings.error.no_merchant"))

        val process = processGateway.findLatestPendingProcessesByTypeAndForUserId(
            processType = ProcessType.ORGANIZATION_SETUP,
            userId = user.id!!
        ) ?: throw RecordNotFoundException(messageService.getMessage("setup.error.no_pending_process"))

        processGateway.makeRequest(
            MakeProcessRequestPayload(
                userId = user.id!!,
                publicId = process.publicId,
                eventType = ProcessEvent.ORGANIZATION_PREFERENCES_SAVED,
                requestType = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
                channel = ProcessChannel.WEB_APP,
                data = mapOf(
                    ProcessRequestDataName.TIMEZONE to command.timezone,
                    ProcessRequestDataName.DATE_FORMAT to command.dateFormat,
                    ProcessRequestDataName.NUMBER_FORMAT to command.numberFormat,
                    ProcessRequestDataName.SETUP_STEP to "preferences"
                )
            )
        )

        log.info { "Preferences step recorded for user: ${user.id}" }

        return SavePreferencesResult(
            success = true,
            message = messageService.getMessage("setup.success.preferences_saved")
        )
    }
}
