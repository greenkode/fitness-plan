package com.krachtix.identity.core.settings.command

import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.service.CacheEvictionService
import com.krachtix.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class UpdateAlertsCommandHandler(
    private val userService: UserService,
    private val organizationRepository: OrganizationRepository,
    private val cacheEvictionService: CacheEvictionService,
    private val messageService: MessageService
) : Command.Handler<UpdateAlertsCommand, UpdateAlertsResult> {

    override fun handle(command: UpdateAlertsCommand): UpdateAlertsResult {

        val user = userService.getCurrentUser()

        val merchantId = user.merchantId
            ?: throw RecordNotFoundException(messageService.getMessage("settings.error.no_merchant"))

        val organization = organizationRepository.findByIdWithProperties(merchantId)
            .orElseThrow { RecordNotFoundException(messageService.getMessage("settings.error.merchant_not_found")) }

        organization.addProperty(OrganizationPropertyName.FAILURE_LIMIT, command.failureLimit.toString())
        organization.addProperty(OrganizationPropertyName.LOW_BALANCE, command.lowBalance.toString())

        organizationRepository.save(organization)

        log.info { "Updated alert settings for organization: ${organization.id}" }

        cacheEvictionService.evictMerchantCaches(merchantId.toString())

        return UpdateAlertsResult(
            success = true,
            message = messageService.getMessage("settings.success.alerts_updated")
        )
    }
}
