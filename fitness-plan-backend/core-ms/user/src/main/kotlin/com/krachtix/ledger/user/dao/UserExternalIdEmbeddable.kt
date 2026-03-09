package com.krachtix.user.dao

import com.krachtix.user.dto.UserExternalId
import jakarta.persistence.Embeddable

@Embeddable
data class UserExternalIdEmbeddable(
    var externalId: String,
    var integratorCode: String,
    var integrator: String
) {

    fun toDomain(): UserExternalId {
        return UserExternalId(externalId, integratorCode, integrator)
    }
}