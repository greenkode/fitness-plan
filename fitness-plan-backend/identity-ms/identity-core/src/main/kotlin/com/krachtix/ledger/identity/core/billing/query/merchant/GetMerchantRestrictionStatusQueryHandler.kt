package com.krachtix.identity.core.billing.query.merchant

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.core.billing.dto.MerchantRestrictionStatusResponse
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetMerchantRestrictionStatusQueryHandler(
    private val organizationRepository: OrganizationRepository,
    private val userService: UserService
) : Command.Handler<GetMerchantRestrictionStatusQuery, GetMerchantRestrictionStatusResult> {

    override fun handle(query: GetMerchantRestrictionStatusQuery): GetMerchantRestrictionStatusResult {
        val merchantId = userService.getCurrentUser().organizationId
            ?: throw IllegalStateException("User has no organization")
        log.debug { "Checking restriction status for merchant=$merchantId" }

        val organization = organizationRepository.findById(merchantId)
            .orElse(null)

        return GetMerchantRestrictionStatusResult(
            response = MerchantRestrictionStatusResponse(
                restricted = organization?.restricted ?: false,
                reason = organization?.restrictionReason
            )
        )
    }
}
