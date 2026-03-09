package com.krachtix.config

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.tenant.TenantContext
import org.hibernate.cfg.AvailableSettings
import org.hibernate.context.spi.CurrentTenantIdentifierResolver
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.stereotype.Component
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class MerchantTenantResolver : CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    companion object {
        val SYSTEM_TENANT: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    override fun resolveCurrentTenantIdentifier(): UUID {
        val merchantId = TenantContext.getMerchantIdOrNull()
        if (merchantId == null) {
            log.trace { "No tenant context available, returning system tenant" }
            return SYSTEM_TENANT
        }
        return merchantId
    }

    override fun validateExistingCurrentSessions(): Boolean = true

    override fun isRoot(tenantId: UUID): Boolean = tenantId == SYSTEM_TENANT

    override fun customize(hibernateProperties: MutableMap<String, Any>) {
        hibernateProperties[AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER] = this
    }
}
