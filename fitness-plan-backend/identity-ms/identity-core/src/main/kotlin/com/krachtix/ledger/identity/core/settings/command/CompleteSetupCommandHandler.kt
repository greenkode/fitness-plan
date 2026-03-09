package com.krachtix.identity.core.settings.command

import com.krachtix.commons.dto.PhoneNumber
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
import com.krachtix.identity.core.currency.domain.OrganizationCurrency
import com.krachtix.identity.core.currency.domain.OrganizationCurrencyRepository
import com.krachtix.identity.core.currency.service.CurrencyLimitService
import com.krachtix.identity.core.entity.OrganizationStatus
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.CacheEvictionService
import com.krachtix.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

private val log = KotlinLogging.logger {}

@Component
@Transactional
class CompleteSetupCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val organizationRepository: OrganizationRepository,
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository,
    private val organizationCurrencyRepository: OrganizationCurrencyRepository,
    private val currencyLimitService: CurrencyLimitService,
    private val processGateway: ProcessGateway,
    private val cacheEvictionService: CacheEvictionService,
    private val messageService: MessageService
) : Command.Handler<CompleteSetupCommand, CompleteSetupResult> {

    override fun handle(command: CompleteSetupCommand): CompleteSetupResult {
        val user = userService.getCurrentUser()

        val merchantId = user.merchantId
            ?: throw RecordNotFoundException(messageService.getMessage("settings.error.no_merchant"))

        val organization = organizationRepository.findByIdWithProperties(merchantId)
            .orElseThrow { RecordNotFoundException(messageService.getMessage("settings.error.merchant_not_found")) }

        val process = processGateway.findLatestPendingProcessesByTypeAndForUserId(
            processType = ProcessType.ORGANIZATION_SETUP,
            userId = user.id!!
        ) ?: throw RecordNotFoundException(messageService.getMessage("setup.error.no_pending_process"))

        val transitions = processGateway.getProcessTransitions(process.publicId)
        if (transitions.none { it.event == ProcessEvent.ORGANIZATION_PROFILE_COMPLETED }) {
            throw InvalidRequestException(messageService.getMessage("setup.error.profile_not_completed"))
        }
        if (transitions.none { it.event == ProcessEvent.ORGANIZATION_CURRENCY_SELECTED }) {
            throw InvalidRequestException(messageService.getMessage("setup.error.currency_not_completed"))
        }
        if (transitions.none { it.event == ProcessEvent.ORGANIZATION_PREFERENCES_SAVED }) {
            throw InvalidRequestException(messageService.getMessage("setup.error.preferences_not_completed"))
        }

        val stepRequests = process.requests
            .filter { it.type == ProcessRequestType.ORGANIZATION_STEP_UPDATE }
            .sortedBy { it.id }

        val profileData = stepRequests.lastOrNull { it.data[ProcessRequestDataName.SETUP_STEP] == "profile" }?.data
            ?: throw InvalidRequestException(messageService.getMessage("setup.error.profile_not_completed"))
        val currencyData = stepRequests.lastOrNull { it.data[ProcessRequestDataName.SETUP_STEP] == "currency" }?.data
            ?: throw InvalidRequestException(messageService.getMessage("setup.error.currency_not_completed"))
        val preferencesData = stepRequests.lastOrNull { it.data[ProcessRequestDataName.SETUP_STEP] == "preferences" }?.data
            ?: throw InvalidRequestException(messageService.getMessage("setup.error.preferences_not_completed"))

        val companyName = profileData[ProcessRequestDataName.COMPANY_NAME] ?: ""
        organization.name = companyName
        organization.addProperty(OrganizationPropertyName.INTENDED_PURPOSE, profileData[ProcessRequestDataName.INTENDED_PURPOSE] ?: "")
        organization.addProperty(OrganizationPropertyName.COMPANY_SIZE, profileData[ProcessRequestDataName.COMPANY_SIZE] ?: "")
        organization.addProperty(OrganizationPropertyName.ROLE_IN_COMPANY, profileData[ProcessRequestDataName.ROLE_IN_COMPANY] ?: "")
        organization.addProperty(OrganizationPropertyName.COUNTRY, profileData[ProcessRequestDataName.COUNTRY] ?: "")
        organization.addProperty(OrganizationPropertyName.PHONE_NUMBER, profileData[ProcessRequestDataName.PHONE_NUMBER_RAW] ?: "")
        profileData[ProcessRequestDataName.WEBSITE_URL]?.let {
            organization.addProperty(OrganizationPropertyName.WEBSITE, it)
        }
        organization.addProperty(OrganizationPropertyName.TERMS_ACCEPTED, "true")

        organization.addProperty(OrganizationPropertyName.DEFAULT_CURRENCY, currencyData[ProcessRequestDataName.DEFAULT_CURRENCY] ?: "")
        organization.addProperty(OrganizationPropertyName.MULTI_CURRENCY_ENABLED, currencyData[ProcessRequestDataName.MULTI_CURRENCY_ENABLED] ?: "false")
        organization.addProperty(OrganizationPropertyName.CHART_TEMPLATE_ID, currencyData[ProcessRequestDataName.CHART_TEMPLATE_ID] ?: "")
        organization.addProperty(OrganizationPropertyName.FISCAL_YEAR_START, currencyData[ProcessRequestDataName.FISCAL_YEAR_START] ?: "")

        organization.addProperty(OrganizationPropertyName.TIMEZONE, preferencesData[ProcessRequestDataName.TIMEZONE] ?: "")
        organization.addProperty(OrganizationPropertyName.DATE_FORMAT, preferencesData[ProcessRequestDataName.DATE_FORMAT] ?: "")
        organization.addProperty(OrganizationPropertyName.NUMBER_FORMAT, preferencesData[ProcessRequestDataName.NUMBER_FORMAT] ?: "")

        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "true")
        organization.status = OrganizationStatus.ACTIVE
        organizationRepository.save(organization)

        val client = oAuthRegisteredClientRepository.findById(merchantId).orElse(null)
        client?.let {
            it.clientName = companyName
            it.status = OrganizationStatus.ACTIVE
            oAuthRegisteredClientRepository.save(it)
        }

        val clientId = client?.clientId ?: merchantId.toString()
        saveOrganizationCurrencies(clientId, currencyData, organization)

        val phoneNumberRaw = profileData[ProcessRequestDataName.PHONE_NUMBER_RAW]
        val country = profileData[ProcessRequestDataName.COUNTRY]
        if (!phoneNumberRaw.isNullOrBlank() && !country.isNullOrBlank()) {
            runCatching {
                val locale = Locale.Builder().setRegion(country).build()
                user.phoneNumber = PhoneNumber.fromRawNumber(phoneNumberRaw, locale)
                userRepository.save(user)
                log.info { "Phone number saved to user profile: ${user.id}" }
            }.onFailure { e ->
                log.warn(e) { "Failed to save phone number to user profile, continuing with setup" }
            }
        }

        processGateway.makeRequest(
            MakeProcessRequestPayload(
                userId = user.id!!,
                publicId = process.publicId,
                eventType = ProcessEvent.PROCESS_COMPLETED,
                requestType = ProcessRequestType.COMPLETE_PROCESS,
                channel = ProcessChannel.WEB_APP
            )
        )

        cacheEvictionService.evictMerchantCaches(merchantId.toString())

        log.info { "Organization setup completed for organization: ${organization.id}" }

        return CompleteSetupResult(
            success = true,
            message = messageService.getMessage("setup.success.completed"),
            merchantId = merchantId.toString()
        )
    }

    private fun saveOrganizationCurrencies(
        clientId: String,
        currencyData: Map<ProcessRequestDataName, String>,
        organization: com.krachtix.identity.core.organization.entity.Organization
    ) {
        val primaryCurrency = currencyData[ProcessRequestDataName.DEFAULT_CURRENCY] ?: return
        val multiCurrencyEnabled = currencyData[ProcessRequestDataName.MULTI_CURRENCY_ENABLED]?.toBoolean() ?: false
        val additionalCurrenciesRaw = currencyData[ProcessRequestDataName.ADDITIONAL_CURRENCIES]

        val existingCurrencies = organizationCurrencyRepository.findByClientId(clientId)
            .associateBy { it.currencyCode }

        val desiredCurrencies = mutableSetOf(primaryCurrency)
        if (multiCurrencyEnabled && !additionalCurrenciesRaw.isNullOrBlank()) {
            desiredCurrencies.addAll(
                additionalCurrenciesRaw.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it != primaryCurrency }
            )
        }

        val newCurrencyCount = desiredCurrencies.count { it !in existingCurrencies || existingCurrencies[it]?.enabled == false }
        if (newCurrencyCount > 0) {
            currencyLimitService.validateCurrencyAddition(clientId, organization, newCurrencyCount)
        }

        desiredCurrencies.forEach { currencyCode ->
            val existing = existingCurrencies[currencyCode]
            val isPrimary = currencyCode == primaryCurrency

            if (existing != null) {
                if (existing.isPrimary != isPrimary || !existing.enabled) {
                    organizationCurrencyRepository.save(
                        OrganizationCurrency(
                            id = existing.id,
                            clientId = clientId,
                            currencyCode = currencyCode,
                            isPrimary = isPrimary,
                            enabled = true
                        )
                    )
                }
            } else {
                organizationCurrencyRepository.save(
                    OrganizationCurrency(
                        clientId = clientId,
                        currencyCode = currencyCode,
                        isPrimary = isPrimary,
                        enabled = true
                    )
                )
            }
        }

        existingCurrencies.values
            .filter { it.currencyCode !in desiredCurrencies && it.enabled }
            .forEach { currency ->
                organizationCurrencyRepository.save(
                    OrganizationCurrency(
                        id = currency.id,
                        clientId = clientId,
                        currencyCode = currency.currencyCode,
                        isPrimary = false,
                        enabled = false
                    )
                )
            }
    }
}
