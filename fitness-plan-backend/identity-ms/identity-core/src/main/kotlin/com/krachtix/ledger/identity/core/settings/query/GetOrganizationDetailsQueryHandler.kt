package com.krachtix.identity.core.settings.query

import com.krachtix.identity.core.currency.domain.OrganizationCurrencyRepository
import com.krachtix.identity.core.entity.CompanyRole
import com.krachtix.identity.core.entity.CompanySize
import com.krachtix.identity.core.entity.EnvironmentMode
import com.krachtix.identity.core.entity.IntendedPurpose
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetOrganizationDetailsQueryHandler(
    private val userService: UserService,
    private val organizationRepository: OrganizationRepository,
    private val oAuthRegisteredClientRepository: OAuthRegisteredClientRepository,
    private val organizationCurrencyRepository: OrganizationCurrencyRepository
) : Command.Handler<GetOrganizationDetailsQuery, GetOrganizationDetailsResult> {

    override fun handle(query: GetOrganizationDetailsQuery): GetOrganizationDetailsResult {
        log.info { "Processing GetOrganizationDetailsQuery" }

        val user = userService.getCurrentUser()
        val merchantId = user.merchantId
            ?: throw IllegalStateException("User is not associated with a merchant")

        val organization = organizationRepository.findByIdWithProperties(merchantId)
            .orElseThrow { IllegalStateException("Organization not found: $merchantId") }

        val client = oAuthRegisteredClientRepository.findById(merchantId).orElse(null)

        val setupCompleted = organization.getProperty(OrganizationPropertyName.SETUP_COMPLETED)?.toBoolean() ?: false
        val intendedPurpose = organization.getProperty(OrganizationPropertyName.INTENDED_PURPOSE)
            ?.let { runCatching { IntendedPurpose.valueOf(it) }.getOrNull() }
        val companySize = organization.getProperty(OrganizationPropertyName.COMPANY_SIZE)
            ?.let { runCatching { CompanySize.valueOf(it) }.getOrNull() }
        val roleInCompany = organization.getProperty(OrganizationPropertyName.ROLE_IN_COMPANY)
            ?.let { runCatching { CompanyRole.valueOf(it) }.getOrNull() }
        val country = organization.getProperty(OrganizationPropertyName.COUNTRY)
        val phoneNumber = organization.getProperty(OrganizationPropertyName.PHONE_NUMBER)
        val website = organization.getProperty(OrganizationPropertyName.WEBSITE)
        val email = organization.getProperty(OrganizationPropertyName.EMAIL)
        val defaultCurrency = organization.getProperty(OrganizationPropertyName.DEFAULT_CURRENCY)
        val multiCurrencyEnabled = organization.getProperty(OrganizationPropertyName.MULTI_CURRENCY_ENABLED)?.toBoolean() ?: false

        val clientId = client?.clientId ?: merchantId.toString()
        val organizationCurrencies = organizationCurrencyRepository.findByClientIdAndEnabledTrue(clientId)
        val additionalCurrencies = organizationCurrencies
            .filter { !it.isPrimary }
            .map { it.currencyCode }

        val chartTemplateId = organization.getProperty(OrganizationPropertyName.CHART_TEMPLATE_ID)
        val fiscalYearStart = organization.getProperty(OrganizationPropertyName.FISCAL_YEAR_START)
        val timezone = organization.getProperty(OrganizationPropertyName.TIMEZONE)
        val dateFormat = organization.getProperty(OrganizationPropertyName.DATE_FORMAT)
        val numberFormat = organization.getProperty(OrganizationPropertyName.NUMBER_FORMAT)

        return GetOrganizationDetailsResult(
            id = organization.id.toString(),
            name = organization.name,
            plan = organization.plan,
            status = organization.status,
            environmentMode = client?.environmentMode ?: EnvironmentMode.SANDBOX,
            setupCompleted = setupCompleted,
            intendedPurpose = intendedPurpose,
            companySize = companySize,
            roleInCompany = roleInCompany,
            country = country,
            phoneNumber = phoneNumber,
            website = website,
            email = email,
            defaultCurrency = defaultCurrency,
            multiCurrencyEnabled = multiCurrencyEnabled,
            additionalCurrencies = additionalCurrencies,
            chartTemplateId = chartTemplateId,
            fiscalYearStart = fiscalYearStart,
            timezone = timezone,
            dateFormat = dateFormat,
            numberFormat = numberFormat
        )
    }
}
