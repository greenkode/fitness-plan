package com.krachtix.commons.model

import jakarta.persistence.AttributeConverter
import org.springframework.util.StringUtils
import javax.money.CurrencyUnit
import javax.money.Monetary


class SetToStringConverter : AttributeConverter<Set<String>, String> {

    override fun convertToDatabaseColumn(set: Set<String>?): String {
        return StringUtils.collectionToCommaDelimitedString(set)
    }

    override fun convertToEntityAttribute(setObjects: String?): Set<String> {
        return StringUtils.commaDelimitedListToSet(setObjects)
    }
}

class CurrencyUnitConverter : AttributeConverter<CurrencyUnit?, String?> {

    override fun convertToDatabaseColumn(currencyUnit: CurrencyUnit?): String? {
        return currencyUnit?.currencyCode
    }

    override fun convertToEntityAttribute(currencyCode: String?): CurrencyUnit? {
        return currencyCode?.let { Monetary.getCurrency(it) }
    }
}
