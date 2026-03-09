package com.krachtix.identity.core.settings.query

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.currency.domain.OrganizationCurrencyRepository
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetMerchantSettingsQueryHandler(
    private val organizationRepository: OrganizationRepository,
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository,
    private val organizationCurrencyRepository: OrganizationCurrencyRepository,
    private val messageService: MessageService
) : Command.Handler<GetMerchantSettingsQuery, GetMerchantSettingsResult> {

    override fun handle(query: GetMerchantSettingsQuery): GetMerchantSettingsResult {
        log.info { "Fetching merchant settings for merchantId=${query.merchantId}" }

        val organization = organizationRepository.findByIdWithProperties(query.merchantId)
            .orElseThrow { RecordNotFoundException(messageService.getMessage("settings.error.merchant_not_found")) }

        val setupCompleted = organization.getProperty(OrganizationPropertyName.SETUP_COMPLETED)?.toBoolean() ?: false

        if (!setupCompleted) {
            throw RecordNotFoundException(messageService.getMessage("settings.error.setup_not_completed"))
        }

        val defaultCurrency = organization.getProperty(OrganizationPropertyName.DEFAULT_CURRENCY)
            ?.takeIf { it.isNotBlank() }
            ?: throw RecordNotFoundException(messageService.getMessage("settings.error.currency_not_configured"))

        val chartTemplateId = organization.getProperty(OrganizationPropertyName.CHART_TEMPLATE_ID)
            ?.takeIf { it.isNotBlank() }
            ?: throw RecordNotFoundException(messageService.getMessage("settings.error.chart_template_not_configured"))

        val client = oAuthRegisteredClientRepository.findById(query.merchantId)
            .orElseThrow { RecordNotFoundException(messageService.getMessage("settings.error.merchant_not_found")) }

        val organizationCurrencies = organizationCurrencyRepository.findByClientIdAndEnabledTrue(client.clientId!!)
        val additionalCurrencies = organizationCurrencies
            .filter { !it.isPrimary }
            .map { it.currencyCode }

        val timezone = organization.getProperty(OrganizationPropertyName.TIMEZONE)

        return GetMerchantSettingsResult(
            defaultCurrency = defaultCurrency,
            additionalCurrencies = additionalCurrencies,
            chartTemplateId = chartTemplateId,
            setupCompleted = true,
            timezone = timezone,
            organizationName = organization.name
        )
    }
}
