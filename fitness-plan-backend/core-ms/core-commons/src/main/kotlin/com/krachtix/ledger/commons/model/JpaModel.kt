package com.krachtix.commons.model

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant


@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class AuditableEntity(
    @Version var version: Long = 0,
    @CreatedDate protected var createdAt: Instant = Instant.now(),
    @CreatedBy protected var createdBy: String? = "system",
    @LastModifiedDate protected var lastModifiedAt: Instant? = null,
    @LastModifiedBy protected var lastModifiedBy: String? = "system"
)