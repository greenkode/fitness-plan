package com.krachtix.identity.core.currency.service

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.currency.domain.OrganizationCurrencyRepository
import com.krachtix.identity.core.organization.entity.Organization
import com.krachtix.identity.core.organization.entity.SubscriptionTier
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class CurrencyLimitService(
    private val organizationCurrencyRepository: OrganizationCurrencyRepository,
    private val messageService: MessageService
) {

    fun getMaxCurrencies(tier: SubscriptionTier): Int = when (tier) {
        SubscriptionTier.TRIAL -> 3
        SubscriptionTier.STARTER -> 5
        SubscriptionTier.PROFESSIONAL -> 10
        SubscriptionTier.ENTERPRISE -> Int.MAX_VALUE
    }

    fun validateCurrencyAddition(clientId: String, organization: Organization, additionalCount: Int) {
        val max = getMaxCurrencies(organization.plan)
        val current = organizationCurrencyRepository.countByClientIdAndEnabledTrue(clientId)
        if (current + additionalCount > max) {
            log.warn { "Currency limit exceeded for org=${organization.id}, tier=${organization.plan}, current=$current, requested=$additionalCount, max=$max" }
            throw InvalidRequestException(
                messageService.getMessage("currency.limit.exceeded")
            )
        }
    }
}
