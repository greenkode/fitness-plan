package com.krachtix.identity.core.settings.command

import com.krachtix.commons.dto.PhoneNumber
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
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
class CompleteOrganizationSetupCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val organizationRepository: OrganizationRepository,
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository,
    private val cacheEvictionService: CacheEvictionService,
    private val messageService: MessageService
) : Command.Handler<CompleteOrganizationSetupCommand, CompleteOrganizationSetupResult> {

    override fun handle(command: CompleteOrganizationSetupCommand): CompleteOrganizationSetupResult {
        val user = userService.getCurrentUser()

        val merchantId = user.merchantId
            ?: throw RecordNotFoundException(messageService.getMessage("settings.error.no_merchant"))

        val organization = organizationRepository.findByIdWithProperties(merchantId)
            .orElseThrow { RecordNotFoundException(messageService.getMessage("settings.error.merchant_not_found")) }

        if (organization.getProperty(OrganizationPropertyName.SETUP_COMPLETED) == "true") {
            throw InvalidRequestException(messageService.getMessage("settings.error.setup_already_completed"))
        }

        if (!command.termsAccepted) {
            throw InvalidRequestException(messageService.getMessage("settings.error.terms_required"))
        }

        organization.name = command.companyName
        organization.status = OrganizationStatus.ACTIVE
        organization.addProperty(OrganizationPropertyName.INTENDED_PURPOSE, command.intendedPurpose.name)
        organization.addProperty(OrganizationPropertyName.COMPANY_SIZE, command.companySize.name)
        organization.addProperty(OrganizationPropertyName.ROLE_IN_COMPANY, command.roleInCompany.name)
        organization.addProperty(OrganizationPropertyName.COUNTRY, command.country)
        organization.addProperty(OrganizationPropertyName.PHONE_NUMBER, command.phoneNumber)
        command.website?.takeIf { it.isNotBlank() && it != "https://" }?.let {
            organization.addProperty(OrganizationPropertyName.WEBSITE, it)
        }
        organization.addProperty(OrganizationPropertyName.TERMS_ACCEPTED, "true")
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "true")

        organizationRepository.save(organization)

        val client = oAuthRegisteredClientRepository.findById(merchantId).orElse(null)
        client?.let {
            it.clientName = command.companyName
            it.status = OrganizationStatus.ACTIVE
            oAuthRegisteredClientRepository.save(it)
        }

        command.phoneNumber.takeIf { it.isNotBlank() }?.let { phoneNumber ->
            runCatching {
                val locale = Locale.Builder().setRegion(command.country).build()
                user.phoneNumber = PhoneNumber.fromRawNumber(phoneNumber, locale)
                userRepository.save(user)
                log.info { "Phone number saved to user profile: ${user.id}" }
            }.onFailure { e ->
                log.warn(e) { "Failed to save phone number to user profile, continuing with setup" }
            }
        }

        log.info { "Organization setup completed for organization: ${organization.id}, name: ${command.companyName}" }

        cacheEvictionService.evictMerchantCaches(merchantId.toString())

        return CompleteOrganizationSetupResult(
            success = true,
            message = messageService.getMessage("settings.success.setup_completed"),
            merchantId = merchantId.toString()
        )
    }
}
