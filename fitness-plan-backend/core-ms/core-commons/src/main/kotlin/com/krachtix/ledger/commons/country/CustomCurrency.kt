package com.krachtix.commons.country

import com.krachtix.commons.country.CurrencyParametersNames.IMAGE_URL
import com.krachtix.commons.country.CurrencyParametersNames.NAME
import com.krachtix.commons.country.CurrencyParametersNames.SYMBOL
import org.javamoney.moneta.CurrencyUnitBuilder
import javax.money.CurrencyContextBuilder
import javax.money.CurrencyUnit


data class CustomCurrency(
    val code: String,
    val numericCode: Int,
    val name: String,
    val majorSingle: String,
    val majorPlural: String,
    val symbol: String,
    val symbolNative: String,
    val minorSingle: String,
    val minorPlural: String,
    val imageUrl: String,
    val type: CurrencyType,
    val isoDigits: Int,
    val decimals: Int,
    val numToBasic: Int,
    val enabled: Boolean,
) {

    fun toCurrencyUnit(): CurrencyUnit {

        val context = CurrencyContextBuilder.of("GstLedger")
            .set(SYMBOL.name, symbol)
            .set(IMAGE_URL.name, imageUrl)
            .set(NAME.name, name)
            .build()

        return CurrencyUnitBuilder.of(code, context)
            .setNumericCode(numericCode)
            .setDefaultFractionDigits(decimals).build()
    }

    fun toSupportedCurrency(): SupportedCurrency {
        return SupportedCurrency(
            id = numericCode,
            code = code,
            name = name,
            symbol = symbol,
            imageUrl = imageUrl
        )
    }
}

enum class CurrencyParametersNames {
    SYMBOL,
    NAME,
    IMAGE_URL,
}

enum class CurrencyType {
    FIAT,
    CRYPTO,
}

data class SupportedCurrency(
    val id: Int,
    val code: String,
    val name: String,
    val symbol: String,
    val imageUrl: String
)
