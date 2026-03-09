package com.krachtix.identity.core.country.controller

import com.krachtix.identity.core.country.domain.CountryRepository
import com.krachtix.identity.core.country.dto.CountryResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/countries")
class CountryController(
    private val countryRepository: CountryRepository
) {

    @GetMapping
    fun getCountries(@RequestParam(required = false) enabled: Boolean?): List<CountryResponse> {
        val countries = enabled?.let {
            countryRepository.findAllByEnabledTrueOrderByNameAsc()
        } ?: countryRepository.findAllByOrderByNameAsc()
        return countries.map { it.toDto() }
    }
}
