package com.krachtix.user.integration.identity

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.dto.UserDetailsDto
import com.krachtix.commons.user.UserResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

private val log = KotlinLogging.logger {}

const val ID_INTEGRATION_RESOURCE = "IDENTITY"
const val ID_INTEGRATION_CODE = "16e40672-6cc1-4f8d-9ba7-aaaa0bd27013"

@Component
class IdentityIntegration(
    @param:Qualifier("identityRestClient") val restClient: RestClient,
) {

    fun getKycUserDetails(id: UUID): UserDetailsDto? {
        val response = restClient.get().uri(
            "/api/userinfo?id={id}", id.toString()
        ).retrieve().body(UserResponse::class.java)

        return response?.toDto()
    }
}