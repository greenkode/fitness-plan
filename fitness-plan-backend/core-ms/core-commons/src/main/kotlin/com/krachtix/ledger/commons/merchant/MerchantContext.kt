package com.krachtix.commons.merchant

import java.time.ZoneId

data class MerchantContextData(
    val settings: MerchantSettingsDto
) {
    val timezone: ZoneId = ZoneId.of(settings.timezone ?: "UTC")
}

object MerchantContext {
    private val contextHolder = ThreadLocal<MerchantContextData?>()

    fun setContext(data: MerchantContextData) {
        contextHolder.set(data)
    }

    fun getContext(): MerchantContextData {
        return contextHolder.get()
            ?: throw MerchantContextException("No merchant context set")
    }

    fun getContextOrNull(): MerchantContextData? {
        return contextHolder.get()
    }

    fun getTimezone(): ZoneId {
        return getContext().timezone
    }

    fun getSettings(): MerchantSettingsDto {
        return getContext().settings
    }

    fun clear() {
        contextHolder.remove()
    }

    fun isSet(): Boolean {
        return contextHolder.get() != null
    }
}

class MerchantContextException(message: String) : RuntimeException(message)
