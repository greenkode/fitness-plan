package com.krachtix.identity.core.settings.command

import com.krachtix.commons.exception.InvalidRequestException
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
import java.net.URI

private val log = KotlinLogging.logger {}

@Component
@Transactional
class SaveBusinessProfileCommandHandler(
    private val userService: UserService,
    private val processGateway: ProcessGateway,
    private val messageService: MessageService
) : Command.Handler<SaveBusinessProfileCommand, SaveBusinessProfileResult> {

    override fun handle(command: SaveBusinessProfileCommand): SaveBusinessProfileResult {
        val user = userService.getCurrentUser()

        user.merchantId
            ?: throw RecordNotFoundException(messageService.getMessage("settings.error.no_merchant"))

        if (!command.termsAccepted) {
            throw InvalidRequestException(messageService.getMessage("settings.error.terms_required"))
        }

        command.website?.takeIf { it.isNotBlank() && it != "https://" }?.let { validateWebsiteUrl(it) }

        val process = processGateway.findLatestPendingProcessesByTypeAndForUserId(
            processType = ProcessType.ORGANIZATION_SETUP,
            userId = user.id!!
        ) ?: throw RecordNotFoundException(messageService.getMessage("setup.error.no_pending_process"))

        val data = buildMap {
            put(ProcessRequestDataName.COMPANY_NAME, command.companyName)
            put(ProcessRequestDataName.INTENDED_PURPOSE, command.intendedPurpose.name)
            put(ProcessRequestDataName.COMPANY_SIZE, command.companySize.name)
            put(ProcessRequestDataName.ROLE_IN_COMPANY, command.roleInCompany.name)
            put(ProcessRequestDataName.COUNTRY, command.country)
            put(ProcessRequestDataName.PHONE_NUMBER_RAW, command.phoneNumber)
            command.website?.takeIf { it.isNotBlank() && it != "https://" }?.let {
                put(ProcessRequestDataName.WEBSITE_URL, it)
            }
            put(ProcessRequestDataName.SETUP_STEP, "profile")
        }

        processGateway.makeRequest(
            MakeProcessRequestPayload(
                userId = user.id!!,
                publicId = process.publicId,
                eventType = ProcessEvent.ORGANIZATION_PROFILE_COMPLETED,
                requestType = ProcessRequestType.ORGANIZATION_STEP_UPDATE,
                channel = ProcessChannel.WEB_APP,
                data = data
            )
        )

        log.info { "Business profile step recorded for user: ${user.id}" }

        return SaveBusinessProfileResult(
            success = true,
            message = messageService.getMessage("setup.success.profile_saved")
        )
    }

    private fun validateWebsiteUrl(url: String) {
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            throw InvalidRequestException(messageService.getMessage("settings.error.invalid_website_url"))
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw InvalidRequestException(messageService.getMessage("settings.error.invalid_website_url"))
        }
        if (uri.host.isNullOrBlank()) {
            throw InvalidRequestException(messageService.getMessage("settings.error.invalid_website_url"))
        }
    }
}
