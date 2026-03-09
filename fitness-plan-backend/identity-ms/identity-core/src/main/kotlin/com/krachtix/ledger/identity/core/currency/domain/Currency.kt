package com.krachtix.identity.core.currency.domain

import com.krachtix.commons.model.AuditableEntity
import com.krachtix.identity.core.currency.dto.CurrencyResponse
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "currency")
class Currency(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    val publicId: UUID = UUID.randomUUID(),

    val name: String,

    val code: String,

    val symbol: String,

    val symbolNative: String,

    val isoNum: Int?,

    val isoDigits: Int,

    val decimals: Int,

    @Enumerated(EnumType.STRING)
    val type: CurrencyType = CurrencyType.FIAT,

    val imageUrl: String? = null,

    val enabled: Boolean = true
) : AuditableEntity() {
    fun toDto() = CurrencyResponse(
        id = publicId,
        code = code,
        name = name,
        symbol = symbol,
        type = type,
        imageUrl = imageUrl
    )
}
