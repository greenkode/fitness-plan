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
class SaveCurrencySettingsCommandHandler(
    private val userService: UserService,
    private val processGateway: ProcessGateway,
    private val messageService: MessageService
) : Command.Handler<SaveCurrencySettingsCommand, SaveCurrencySettingsResult> {

    override fun handle(command: SaveCurrencySettingsCommand): SaveCurrencySettingsResult {
        val user = userService.getCurrentUser()

        user.merchantId
            ?: throw RecordNotFoundException(messageService.getMessage("settings.error.no_merchant"))

        val process = processGateway.findLatestPendingProcessesByTypeAndForUserId(
            processType = ProcessType.ORGANIZATION_SETUP,
            userId = user.id!!
        ) ?: throw RecordNotFoundException(messageService.getMessage("setup.error.no_pending_process"))

        val data = buildMap {
            put(ProcessRequestDataName.DEFAULT_CURRENCY, command.primaryCurrency)
            put(ProcessRequestDataName.MULTI_CURRENCY_ENABLED, command.multiCurrencyEnabled.toString())
            put(ProcessRequestDataName.CHART_TEMPLATE_ID, command.chartTemplateId)
            put(ProcessRequestDataName.FISCAL_YEAR_START, command.fiscalYearStart)
            if (command.multiCurrencyEnabled && command.additionalCurrencies.isNotEmpty()) {
                put(ProcessRequestDataName.ADDITIONAL_CURRENCIES, command.additionalCurrencies.joinToString(","))
            }
            put(ProcessRequestDataName.SETUP_STEP, "currency")
        }

        processGateway.makeRequest(
            MakeProcessRequestPayload(
                userId = user.id!!,
                publicId = process.publicId,
                eventType = ProcessEvent.ORGANIZATION_CURRENCY_SELECTED,
                requestType = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
                channel = ProcessChannel.WEB_APP,
                data = data
            )
        )

        log.info { "Currency settings step recorded for user: ${user.id}" }

        return SaveCurrencySettingsResult(
            success = true,
            message = messageService.getMessage("setup.success.currency_saved")
        )
    }
}
