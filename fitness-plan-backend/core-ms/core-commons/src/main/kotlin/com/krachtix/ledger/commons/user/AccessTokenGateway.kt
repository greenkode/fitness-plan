package com.krachtix.user

import com.krachtix.user.dto.AccessTokenDto

interface AccessTokenGateway {

    fun findLatestByInstitutionAndResource(institution: String, resource: String): AccessTokenDto?

    fun save(dto: AccessTokenDto): AccessTokenDto
}