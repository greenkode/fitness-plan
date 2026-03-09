package com.krachtix.identity.core.currency.dto

import com.krachtix.identity.core.currency.domain.CurrencyType
import java.util.UUID

data class CurrencyResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val symbol: String,
    val type: CurrencyType,
    val imageUrl: String?
)
