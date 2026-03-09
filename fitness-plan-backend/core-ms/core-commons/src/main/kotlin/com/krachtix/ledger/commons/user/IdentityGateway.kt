package com.krachtix.commons.user

import com.krachtix.commons.dto.UserDetailsDto
import java.util.UUID

interface IdentityGateway {

    fun getUserKycDetails(publicId: UUID) : UserDetailsDto?
}