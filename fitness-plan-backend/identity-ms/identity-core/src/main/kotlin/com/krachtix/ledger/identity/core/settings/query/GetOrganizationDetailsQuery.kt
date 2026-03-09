package com.krachtix.identity.core.settings.query

import com.krachtix.identity.core.organization.entity.SubscriptionTier
import com.krachtix.identity.core.entity.CompanyRole
import com.krachtix.identity.core.entity.CompanySize
import com.krachtix.identity.core.entity.EnvironmentMode
import com.krachtix.identity.core.entity.IntendedPurpose
import com.krachtix.identity.core.entity.OrganizationStatus
import an.awesome.pipelinr.Command

class GetOrganizationDetailsQuery : Command<GetOrganizationDetailsResult>

data class GetOrganizationDetailsResult(
    val id: String,
    val name: String,
    val plan: SubscriptionTier,
    val status: OrganizationStatus,
    val environmentMode: EnvironmentMode,
    val setupCompleted: Boolean,
    val intendedPurpose: IntendedPurpose?,
    val companySize: CompanySize?,
    val roleInCompany: CompanyRole?,
    val country: String?,
    val phoneNumber: String?,
    val website: String?,
    val email: String?,
    val defaultCurrency: String?,
    val multiCurrencyEnabled: Boolean,
    val additionalCurrencies: List<String>,
    val chartTemplateId: String?,
    val fiscalYearStart: String?,
    val timezone: String?,
    val dateFormat: String?,
    val numberFormat: String?
)
