package com.krachtix.user.spi

import com.krachtix.user.AccessTokenGateway
import com.krachtix.user.dto.AccessTokenDto
import com.krachtix.user.dao.AccessTokenEntity
import com.krachtix.user.dao.AccessTokenRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class AccessTokenService(private val accessTokenRepository: AccessTokenRepository) : AccessTokenGateway {

    override fun findLatestByInstitutionAndResource(institution: String, resource: String): AccessTokenDto? {

        return accessTokenRepository.findFirstByInstitutionAndResourceAndExpiryAfterOrderByExpiryDesc(
            institution, resource,
            Instant.now()
        )?.toDomain()?.toDto()
    }

    override fun save(dto: AccessTokenDto): AccessTokenDto {

        return accessTokenRepository.save(
            AccessTokenEntity(
                dto.type,
                dto.expiry,
                dto.accessToken,
                dto.refreshToken,
                dto.resource,
                dto.institution
            )
        ).toDomain().toDto()
    }
}