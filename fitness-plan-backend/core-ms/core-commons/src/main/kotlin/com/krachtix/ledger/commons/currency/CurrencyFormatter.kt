package com.krachtix.commons.currency

import org.javamoney.moneta.Money
import javax.money.CurrencyUnit
import javax.money.format.MonetaryFormats
import java.util.Locale

object CurrencyFormatter {
    fun getSymbol(currency: CurrencyUnit, locale: Locale = Locale.getDefault()): String {
        return try {
            val formatter = MonetaryFormats.getAmountFormat(locale)
            val formatted = formatter.format(Money.of(0, currency))
            formatted.replace(Regex("[0-9.,\\s]+"), "").trim()
        } catch (e: Exception) {
            val symbolMap = mapOf(
                "NGN" to "₦",
                "USD" to "$",
                "EUR" to "€",
                "GBP" to "£",
                "JPY" to "¥",
                "INR" to "₹"
            )
            symbolMap[currency.currencyCode] ?: currency.currencyCode
        }
    }
}
