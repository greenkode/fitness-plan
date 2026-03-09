package com.krachtix.identity.core.currency.domain

import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CurrencyRepository : JpaRepository<Currency, Int> {

    @Cacheable(cacheNames = ["currencies"], key = "'enabled_currencies'")
    fun findAllByEnabledTrueOrderByNameAsc(): List<Currency>

    @Cacheable(cacheNames = ["currencies"], key = "'all_currencies'")
    fun findAllByOrderByNameAsc(): List<Currency>

    fun findByCode(code: String): Currency?

    fun findByPublicId(publicId: UUID): Currency?
}
