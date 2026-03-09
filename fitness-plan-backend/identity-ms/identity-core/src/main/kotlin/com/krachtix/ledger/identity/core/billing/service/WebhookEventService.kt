package com.krachtix.identity.core.billing.service

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.commons.payment.WebhookEventGateway
import com.krachtix.identity.commons.payment.WebhookPaymentEventRequest
import com.krachtix.identity.core.integration.CoreBillingClient
import com.krachtix.identity.core.integration.PaymentEventRequest
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class WebhookEventService(
    private val organizationRepository: OrganizationRepository,
    private val coreBillingClient: CoreBillingClient
) : WebhookEventGateway {

    override fun recordPaymentEvent(request: WebhookPaymentEventRequest) {
        coreBillingClient.recordPaymentEvent(
            PaymentEventRequest(
                externalEventId = request.externalEventId,
                eventType = request.eventType,
                merchantId = request.merchantId,
                invoicePublicId = request.invoicePublicId,
                amountCents = request.amountCents,
                currency = request.currency,
                rawPayload = request.rawPayload
            )
        )
    }

    @Transactional
    override fun liftRestriction(merchantId: UUID) {
        organizationRepository.findById(merchantId).ifPresent { org ->
            if (org.restricted) {
                org.restricted = false
                org.restrictedAt = null
                org.restrictionReason = null
                org.updatedAt = Instant.now()
                organizationRepository.save(org)
                log.info { "Lifted restriction for merchant $merchantId after payment" }
            }
        }
    }

    @Transactional
    override fun updateSubscription(merchantId: UUID, subscriptionId: String) {
        organizationRepository.findById(merchantId).ifPresent { org ->
            org.stripeSubscriptionId = subscriptionId
            org.updatedAt = Instant.now()
            organizationRepository.save(org)
            log.info { "Updated subscription for merchant $merchantId: $subscriptionId" }
        }
    }
}
