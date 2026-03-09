package com.krachtix.identity.core.auth.command

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.TwoFactorAuthenticationRequiredException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.audit.AuditEvent
import com.krachtix.identity.commons.audit.AuditPayloadKey.OLD_JTI
import com.krachtix.identity.commons.audit.AuditPayloadKey.USERNAME
import com.krachtix.identity.commons.audit.AuditPayloadKey.USER_ID
import com.krachtix.identity.commons.audit.AuditResource
import com.krachtix.identity.commons.audit.IdentityType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.auth.service.JwtTokenService
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.CustomUserDetails
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional(noRollbackFor = [TwoFactorAuthenticationRequiredException::class])
class RefreshTokenCommandHandler(
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val userRepository: OAuthUserRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val messageService: MessageService,
    private val processGateway: ProcessGateway,
    @Value("\${identity.2fa.reverification-interval-hours:24}")
    private val reverificationIntervalHours: Long = 24
) : Command.Handler<RefreshTokenCommand, RefreshTokenResult> {

    override fun handle(command: RefreshTokenCommand): RefreshTokenResult {
        log.info { "Refresh token request received" }

        val storedToken = refreshTokenService.validateAndGetRefreshTokenByHash(command.refreshToken)

        val user = userRepository.findById(storedToken.userId)
            .orElseThrow { InvalidRequestException(messageService.getMessage("auth.error.user_not_found")) }

        if (!user.emailVerified) {
            throw InvalidRequestException(messageService.getMessage("auth.error.email_not_verified"))
        }

        if (user.totpEnabled) {
            val lastVerified = user.twoFactorLastVerified
            val isStale = lastVerified == null ||
                lastVerified.isBefore(Instant.now().minus(Duration.ofHours(reverificationIntervalHours)))

            if (isStale) {
                val sessionId = RandomStringUtils.secure().nextAlphanumeric(100)

                processGateway.createProcess(
                    CreateNewProcessPayload(
                        userId = user.id!!,
                        publicId = UUID.randomUUID(),
                        type = ProcessType.TWO_FACTOR_AUTH,
                        description = "TOTP re-verification for ${user.email?.value}",
                        initialState = ProcessState.PENDING,
                        requestState = ProcessState.COMPLETE,
                        channel = ProcessChannel.BUSINESS_WEB,
                        externalReference = sessionId,
                        data = mapOf(
                            ProcessRequestDataName.USER_IDENTIFIER to user.id.toString(),
                            ProcessRequestDataName.TWO_FACTOR_METHOD to "TOTP"
                        ),
                        stakeholders = mapOf(
                            ProcessStakeholderType.FOR_USER to user.id.toString()
                        )
                    )
                )

                val restrictedToken = jwtTokenService.generateRestrictedTwoFactorToken(user)

                throw TwoFactorAuthenticationRequiredException(
                    sessionId = sessionId,
                    message = messageService.getMessage("twofactor.reverification.required"),
                    twoFactorMethod = "TOTP",
                    restrictedToken = restrictedToken
                )
            }
        }

        val userDetails = CustomUserDetails(user)
        val newAccessToken = jwtTokenService.generateToken(user, userDetails)
        val newRefreshToken = refreshTokenService.rotateRefreshToken(storedToken, user)

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = user.id.toString(),
                actorName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.email?.value ?: "" },
                merchantId = user.merchantId?.toString() ?: "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "Access token refreshed",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    USERNAME.value to user.username,
                    OLD_JTI.value to storedToken.jti,
                    USER_ID.value to user.id.toString()
                )
            )
        )

        log.info { "Token refresh successful for user: ${user.username}" }

        return RefreshTokenResult(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = jwtTokenService.getTokenExpirySeconds()
        )
    }
}
