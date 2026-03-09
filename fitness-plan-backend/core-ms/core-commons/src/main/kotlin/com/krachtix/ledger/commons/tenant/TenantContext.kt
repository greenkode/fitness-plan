package com.krachtix.commons.tenant

import java.util.UUID

data class TenantContextData(
    val merchantId: UUID
)

object TenantContext {
    private val contextHolder = ThreadLocal<TenantContextData?>()

    fun setContext(data: TenantContextData) {
        contextHolder.set(data)
    }

    fun getContext(): TenantContextData {
        return contextHolder.get()
            ?: throw TenantContextException("No tenant context set")
    }

    fun getContextOrNull(): TenantContextData? {
        return contextHolder.get()
    }

    fun getMerchantId(): UUID {
        return getContext().merchantId
    }

    fun getMerchantIdOrNull(): UUID? {
        return contextHolder.get()?.merchantId
    }

    fun clear() {
        contextHolder.remove()
    }

    fun isSet(): Boolean {
        return contextHolder.get() != null
    }
}

class TenantContextException(message: String) : RuntimeException(message)
