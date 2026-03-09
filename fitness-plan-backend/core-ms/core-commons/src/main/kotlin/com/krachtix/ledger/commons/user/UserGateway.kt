package com.krachtix.commons.user

import com.krachtix.commons.dto.UserDetailsDto
import java.util.UUID

interface UserGateway {

    fun getLoggedInUserId(): UUID?

    fun getAuthenticatedUserClaims(): Map<String, Any>

    fun getSystemUserId(): UUID

    fun findByPublicId(id: UUID): UserDetailsDto?

    fun getLoggedInUserDetails(): UserDetailsDto?

    fun getUserDetailsById(id: UUID): UserDetailsDto?

    fun authorizeAction(userId: UUID, pin: String): Boolean
}