package com.krachtix.notification.dao

import com.krachtix.commons.webhook.WebhookDeliveryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface WebhookDeliveryRepository : JpaRepository<WebhookDeliveryEntity, Long> {

    fun findByMerchantIdOrderByCreatedAtDesc(merchantId: UUID, pageable: Pageable): Page<WebhookDeliveryEntity>

    @Query("SELECT w FROM WebhookDeliveryEntity w WHERE w.status IN :statuses AND w.nextRetryAt <= :now ORDER BY w.nextRetryAt ASC")
    fun findPendingRetries(statuses: List<WebhookDeliveryStatus>, now: Instant, pageable: Pageable): List<WebhookDeliveryEntity>
}
