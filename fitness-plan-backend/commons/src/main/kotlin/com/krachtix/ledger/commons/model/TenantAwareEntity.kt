package com.krachtix.commons.model

import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.TenantId
import java.util.UUID

@MappedSuperclass
open class TenantAwareEntity(
    @TenantId
    open var merchantId: UUID? = null
) : AuditableEntity()
