package com.krachtix.identity.core.settings.query

import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class GetSetupStatusQueryHandler(
    private val userService: UserService,
    private val processGateway: ProcessGateway,
    private val organizationRepository: OrganizationRepository
) : Command.Handler<GetSetupStatusQuery, GetSetupStatusResult> {

    override fun handle(query: GetSetupStatusQuery): GetSetupStatusResult {
        val user = userService.getCurrentUser()

        log.info { "Getting setup status for user: ${user.id}" }

        val organization = user.merchantId?.let { merchantId ->
            organizationRepository.findByIdWithProperties(merchantId).orElse(null)
        }

        if (organization?.getProperty(OrganizationPropertyName.SETUP_COMPLETED) == "true") {
            log.info { "Setup already completed for merchant: ${user.merchantId}" }
            return GetSetupStatusResult(
                processId = null,
                currentStep = 0,
                completedSteps = emptyList(),
                isComplete = true
            )
        }

        val process = processGateway.findLatestPendingProcessesByTypeAndForUserId(
            processType = ProcessType.ORGANIZATION_SETUP,
            userId = user.id!!
        ) ?: createOrganizationSetupProcess(user)

        val transitions = processGateway.getProcessTransitions(process.publicId)
        val completedSteps = mutableListOf<String>()

        if (transitions.any { it.event == ProcessEvent.ORGANIZATION_PROFILE_COMPLETED }) {
            completedSteps.add("profile")
        }
        if (transitions.any { it.event == ProcessEvent.ORGANIZATION_CURRENCY_SELECTED }) {
            completedSteps.add("currency")
        }
        if (transitions.any { it.event == ProcessEvent.ORGANIZATION_PREFERENCES_SAVED }) {
            completedSteps.add("preferences")
        }

        val currentStep = completedSteps.size

        val stepData = mutableMapOf<String, Map<String, String>>()
        val stepRequests = process.requests
            .filter { it.type == ProcessRequestType.ORGANIZATION_STEP_UPDATE }
            .sortedBy { it.id }

        stepRequests.forEach { request ->
            val stepName = request.data[ProcessRequestDataName.SETUP_STEP]
            if (stepName != null) {
                stepData[stepName] = request.data
                    .filterKeys { it != ProcessRequestDataName.SETUP_STEP }
                    .mapKeys { (key, _) -> key.name }
            }
        }

        if (!stepData.containsKey("profile") && organization != null) {
            stepData["profile"] = mapOf(
                ProcessRequestDataName.COMPANY_NAME.name to organization.name
            )
        }

        return GetSetupStatusResult(
            processId = process.publicId,
            currentStep = currentStep,
            completedSteps = completedSteps,
            isComplete = false,
            stepData = stepData
        )
    }

    private fun createOrganizationSetupProcess(user: OAuthUser) = processGateway.createProcess(
        CreateNewProcessPayload(
            userId = user.id!!,
            publicId = UUID.randomUUID(),
            type = ProcessType.ORGANIZATION_SETUP,
            description = "Organization setup for ${user.email?.value}",
            initialState = ProcessState.PENDING,
            requestState = ProcessState.PENDING,
            channel = ProcessChannel.WEB_APP,
            data = mapOf(
                ProcessRequestDataName.USER_IDENTIFIER to user.id.toString(),
                ProcessRequestDataName.MERCHANT_ID to (user.merchantId?.toString() ?: ""),
                ProcessRequestDataName.ORGANIZATION_ID to (user.organizationId?.toString() ?: "")
            ),
            stakeholders = mapOf(
                ProcessStakeholderType.FOR_USER to user.id.toString()
            )
        )
    ).also {
        log.info { "Created new ORGANIZATION_SETUP process ${it.publicId} for user ${user.id}" }
    }
}
