package com.krachtix.util

import com.krachtix.commons.country.CurrencyParametersNames.IMAGE_URL
import com.krachtix.commons.country.CurrencyParametersNames.NAME
import com.krachtix.commons.country.CurrencyParametersNames.SYMBOL
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount


fun CurrencyUnit.isFiat(): Boolean {
    return currencyCode in Currency.getAvailableCurrencies().map { it.currencyCode }
}

fun CurrencyUnit.isCrypto(): Boolean {
    return !this.isFiat()
}


fun CurrencyUnit.symbol(): String {
    return context.getText(SYMBOL.name)
}

fun CurrencyUnit.name(): String {
    return context.getText(NAME.name)
}

fun CurrencyUnit.imageUrl(): String {
    return context.getText(IMAGE_URL.name)
}


fun MonetaryAmount.formatWithSymbol(locale: Locale, fractions: Int = 2): String {

    val currencyUnit = this.currency

    val symbol = currencyUnit.context["SYMBOL", String::class.java] ?: currencyUnit.currencyCode

    val numberFormat = NumberFormat.getNumberInstance(locale) as DecimalFormat
    numberFormat.minimumFractionDigits = fractions
    numberFormat.maximumFractionDigits = fractions

    return "$symbol${numberFormat.format(this.number.numberValueExact(java.math.BigDecimal::class.java))}"
}
