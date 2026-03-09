package com.krachtix.identity.core.billing.service

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.billing.dto.ActivePlanWithOrgResponse
import com.krachtix.identity.core.billing.dto.BillingPlanInternalResponse
import com.krachtix.identity.core.billing.dto.CreateBillingPlanRequest
import com.krachtix.identity.core.billing.dto.UpdateBillingPlanRequest
import com.krachtix.identity.core.billing.entity.BillingPlanEntity
import com.krachtix.identity.core.billing.entity.BillingPlanStatus
import com.krachtix.identity.core.billing.repository.BillingPlanRepository
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class BillingPlanService(
    private val billingPlanRepository: BillingPlanRepository,
    private val organizationRepository: OrganizationRepository,
    private val clientRepository: OAuthRegisteredClientRepository,
    private val messageService: MessageService
) {

    @Transactional
    fun createPlan(request: CreateBillingPlanRequest): BillingPlanEntity {
        log.info { "Creating billing plan for organization: ${request.organizationId}" }

        val organization = organizationRepository.findById(request.organizationId)
            .orElseThrow { RecordNotFoundException(messageService.getMessage("billing.error.organization_not_found")) }

        billingPlanRepository.findByOrganizationIdAndStatus(request.organizationId, BillingPlanStatus.ACTIVE)
            ?.let { existing ->
                log.info { "Cancelling existing active plan ${existing.publicId} for organization ${request.organizationId}" }
                existing.status = BillingPlanStatus.CANCELLED
                existing.effectiveUntil = Instant.now()
                billingPlanRepository.save(existing)
            }

        val plan = BillingPlanEntity(
            organization = organization,
            name = request.name,
            platformFeeAmount = request.platformFeeAmount,
            perAccountFeeAmount = request.perAccountFeeAmount,
            perTransactionFeeAmount = request.perTransactionFeeAmount,
            maxChargeAmount = request.maxChargeAmount,
            currency = request.currency,
            billingCycle = request.billingCycle,
            effectiveFrom = request.effectiveFrom,
            effectiveUntil = request.effectiveUntil
        )

        val saved = billingPlanRepository.save(plan)
        log.info { "Created billing plan ${saved.publicId} for organization ${request.organizationId}" }
        return saved
    }

    @Transactional
    fun updatePlan(publicId: UUID, request: UpdateBillingPlanRequest): BillingPlanEntity {
        log.info { "Updating billing plan: $publicId" }

        val plan = billingPlanRepository.findByPublicId(publicId)
            ?: throw RecordNotFoundException(messageService.getMessage("billing.error.plan_not_found"))

        request.name?.let { plan.name = it }
        request.platformFeeAmount?.let { plan.platformFeeAmount = it }
        request.perAccountFeeAmount?.let { plan.perAccountFeeAmount = it }
        request.perTransactionFeeAmount?.let { plan.perTransactionFeeAmount = it }
        request.maxChargeAmount?.let { plan.maxChargeAmount = it }
        request.currency?.let { plan.currency = it }
        request.billingCycle?.let { plan.billingCycle = it }
        request.effectiveUntil?.let { plan.effectiveUntil = it }

        val saved = billingPlanRepository.save(plan)
        log.info { "Updated billing plan: $publicId" }
        return saved
    }

    @Transactional
    fun deactivatePlan(publicId: UUID): BillingPlanEntity {
        log.info { "Deactivating billing plan: $publicId" }

        val plan = billingPlanRepository.findByPublicId(publicId)
            ?: throw RecordNotFoundException(messageService.getMessage("billing.error.plan_not_found"))

        plan.status = BillingPlanStatus.CANCELLED
        plan.effectiveUntil = Instant.now()

        val saved = billingPlanRepository.save(plan)
        log.info { "Deactivated billing plan: $publicId" }
        return saved
    }

    fun getActivePlan(organizationId: UUID): BillingPlanEntity? =
        billingPlanRepository.findByOrganizationIdAndStatus(organizationId, BillingPlanStatus.ACTIVE)

    fun getPlan(publicId: UUID): BillingPlanEntity? =
        billingPlanRepository.findByPublicId(publicId)

    fun listPlans(organizationId: UUID?, status: BillingPlanStatus?, pageable: Pageable): Page<BillingPlanEntity> =
        when {
            organizationId != null && status != null ->
                billingPlanRepository.findAllByOrganizationIdAndStatus(organizationId, status, pageable)
            status != null ->
                billingPlanRepository.findAllByStatus(status, pageable)
            else ->
                billingPlanRepository.findAll(pageable)
        }

    fun getActivePlanInternal(organizationId: UUID): BillingPlanInternalResponse? =
        billingPlanRepository.findByOrganizationIdAndStatus(organizationId, BillingPlanStatus.ACTIVE)
            ?.toInternalResponse()

    fun getAllActivePlansWithMerchant(): List<ActivePlanWithOrgResponse> {
        val activePlans = billingPlanRepository.findAllByStatus(
            BillingPlanStatus.ACTIVE,
            Pageable.unpaged()
        ).content

        return activePlans.mapNotNull { plan ->
            val orgId = plan.organization.id ?: return@mapNotNull null
            val merchant = clientRepository.findByOrganizationId(orgId).firstOrNull()
            ActivePlanWithOrgResponse(
                organizationId = orgId,
                merchantId = merchant?.id,
                plan = plan.toInternalResponse()
            )
        }
    }

    private fun BillingPlanEntity.toInternalResponse() = BillingPlanInternalResponse(
        publicId = publicId,
        organizationId = organization.id,
        name = name,
        platformFeeAmount = platformFeeAmount,
        perAccountFeeAmount = perAccountFeeAmount,
        perTransactionFeeAmount = perTransactionFeeAmount,
        maxChargeAmount = maxChargeAmount,
        currency = currency,
        billingCycle = billingCycle,
        status = status,
        effectiveFrom = effectiveFrom,
        effectiveUntil = effectiveUntil,
        subscriptionTier = subscriptionTier?.name,
        stripePriceId = stripePriceId
    )
}
