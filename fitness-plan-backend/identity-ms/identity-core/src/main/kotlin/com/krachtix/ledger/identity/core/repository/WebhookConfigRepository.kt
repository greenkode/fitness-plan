package com.krachtix.identity.core.repository

import com.krachtix.identity.core.entity.WebhookConfigEntity
import com.krachtix.identity.core.entity.WebhookConfigStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WebhookConfigRepository : JpaRepository<WebhookConfigEntity, Long> {

    fun findByPublicId(publicId: UUID): WebhookConfigEntity?

    fun findByClientIdAndStatus(clientId: UUID, status: WebhookConfigStatus): List<WebhookConfigEntity>

    fun findByClientId(clientId: UUID, pageable: Pageable): Page<WebhookConfigEntity>

    fun findByClientIdAndStatusAndEventTypesContaining(
        clientId: UUID,
        status: WebhookConfigStatus,
        eventType: String
    ): List<WebhookConfigEntity>

    fun countByClientIdAndStatusNot(clientId: UUID, status: WebhookConfigStatus): Long
}
