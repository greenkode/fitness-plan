package com.krachtix.identity.core.settings.query

import com.krachtix.identity.core.entity.EnvironmentMode
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.settings.dto.EnvironmentStatusResponse
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetEnvironmentStatusQueryHandler(
    private val userService: UserService,
    private val clientRepository: OAuthRegisteredClientRepository
) : Command.Handler<GetEnvironmentStatusQuery, EnvironmentStatusResponse> {

    override fun handle(query: GetEnvironmentStatusQuery): EnvironmentStatusResponse {
        val user = userService.getCurrentUser()
        log.info { "Getting environment status for user: ${user.id}" }

        val merchant = user.merchantId?.let { clientRepository.findById(it).orElse(null) }

        val environmentPreference = user.environmentPreference
        val merchantEnvironmentMode = merchant?.environmentMode ?: EnvironmentMode.SANDBOX
        val lastSwitchedAt = user.environmentLastSwitchedAt

        val effectiveEnvironment = determineEffectiveEnvironment(
            userPreference = environmentPreference,
            merchantMode = merchantEnvironmentMode
        )

        return EnvironmentStatusResponse(
            currentEnvironment = effectiveEnvironment,
            environmentPreference = environmentPreference,
            merchantEnvironmentMode = merchantEnvironmentMode,
            lastSwitchedAt = lastSwitchedAt,
            canSwitchToProduction = merchantEnvironmentMode == EnvironmentMode.PRODUCTION
        )
    }

    private fun determineEffectiveEnvironment(userPreference: EnvironmentMode, merchantMode: EnvironmentMode): EnvironmentMode {
        return if (merchantMode == EnvironmentMode.PRODUCTION) userPreference else merchantMode
    }
}
