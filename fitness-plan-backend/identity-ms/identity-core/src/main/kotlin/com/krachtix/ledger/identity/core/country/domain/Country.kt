package com.krachtix.identity.core.country.domain

import com.krachtix.commons.model.AuditableEntity
import com.krachtix.identity.core.country.dto.CountryResponse
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.util.UUID

@Entity
class Country(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(name = "public_id", nullable = false)
    val publicId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String,

    @Column(name = "iso2_code", nullable = false)
    val iso2Code: String,

    @Column(name = "iso3_code", nullable = false)
    val iso3Code: String,

    @Column(name = "numeric_code", nullable = false)
    val numericCode: String,

    @Column(name = "dial_code", nullable = false)
    val dialCode: String,

    @Column(name = "flag_url", nullable = false)
    val flagUrl: String,

    val region: String = "",

    @Column(name = "sub_region")
    val subRegion: String = "",

    @Column(name = "default_currency_code")
    val defaultCurrencyCode: String? = null,

    val enabled: Boolean = false
) : AuditableEntity() {

    fun toDto() = CountryResponse(
        id = publicId,
        name = name,
        iso2Code = iso2Code,
        iso3Code = iso3Code,
        dialCode = dialCode,
        flagUrl = flagUrl,
        region = region,
        subRegion = subRegion,
        defaultCurrencyCode = defaultCurrencyCode
    )
}
