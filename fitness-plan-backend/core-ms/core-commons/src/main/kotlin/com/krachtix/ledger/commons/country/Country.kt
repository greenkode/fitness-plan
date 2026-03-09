package com.krachtix.commons.country

import java.util.Locale
import java.util.UUID

data class Country(
    val name: String,
    val code: String,
    val dialCode: String,
    val locale: Locale,
    val publicId: UUID,
    val currencies: Set<CountryCustomCurrency>
)