package com.krachtix.identity.core.currency.controller

import com.krachtix.identity.core.currency.domain.CurrencyRepository
import com.krachtix.identity.core.currency.dto.CurrencyResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/currencies")
class CurrencyController(
    private val currencyRepository: CurrencyRepository
) {

    @GetMapping
    fun getCurrencies(@RequestParam(required = false) enabled: Boolean?): List<CurrencyResponse> {
        val currencies = if (enabled == true) {
            currencyRepository.findAllByEnabledTrueOrderByNameAsc()
        } else {
            currencyRepository.findAllByOrderByNameAsc()
        }
        return currencies.map { it.toDto() }
    }
}
