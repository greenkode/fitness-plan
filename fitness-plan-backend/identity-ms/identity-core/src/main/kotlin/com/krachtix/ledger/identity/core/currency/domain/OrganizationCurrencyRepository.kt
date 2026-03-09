package com.krachtix.identity.core.currency.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrganizationCurrencyRepository : JpaRepository<OrganizationCurrency, Int> {

    fun findByClientIdAndEnabledTrue(clientId: String): List<OrganizationCurrency>

    fun findByClientId(clientId: String): List<OrganizationCurrency>

    fun findByClientIdAndIsPrimaryTrue(clientId: String): OrganizationCurrency?

    fun findByClientIdAndCurrencyCode(clientId: String, currencyCode: String): OrganizationCurrency?

    fun countByClientIdAndEnabledTrue(clientId: String): Long
}
