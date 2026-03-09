package com.krachtix.identity.core.auth.command

import com.krachtix.identity.commons.audit.AuditEvent
import com.krachtix.identity.commons.audit.AuditPayloadKey.CODE
import com.krachtix.identity.commons.audit.AuditPayloadKey.PROCESS_ID
import com.krachtix.identity.commons.audit.AuditPayloadKey.SESSION_ID
import com.krachtix.identity.commons.audit.AuditPayloadKey.USERNAME
import com.krachtix.identity.commons.audit.AuditPayloadKey.USER_ID
import com.krachtix.identity.commons.audit.AuditResource
import com.krachtix.identity.commons.audit.IdentityType
import com.krachtix.commons.exception.TwoFactorCodeInvalidException
import com.krachtix.commons.exception.TwoFactorSessionInvalidException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.MakeProcessRequestPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.AccountLockoutService
import com.krachtix.identity.core.service.CustomUserDetails
import com.krachtix.identity.core.auth.service.JwtTokenService
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeRepository
import com.krachtix.identity.core.totp.service.TotpService
import com.krachtix.identity.core.trusteddevice.dto.TrustDeviceCommand
import com.krachtix.identity.core.trusteddevice.service.DeviceFingerprintService
import an.awesome.pipelinr.Command
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

private val log = KotlinLogging.logger {}

@Component
@Transactional
class VerifyTwoFactorCommandHandler(
    private val processGateway: ProcessGateway,
    private val userRepository: OAuthUserRepository,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val pipeline: Pipeline,
    private val deviceFingerprintService: DeviceFingerprintService,
    private val totpService: TotpService,
    private val recoveryCodeRepository: TotpRecoveryCodeRepository,
    private val accountLockoutService: AccountLockoutService,
    private val messageService: MessageService,
    @Value("\${identity.2fa.max-attempts:3}") private val maxAttempts: Int = 3,
    @Value("\${identity.trusted-device.enabled:true}") private val trustedDeviceEnabled: Boolean = true,
    @Value("\${identity.trusted-device.default-duration-days:30}") private val defaultTrustDurationDays: Int = 30
) : Command.Handler<VerifyTwoFactorCommand, VerifyTwoFactorResult> {

    override fun handle(command: VerifyTwoFactorCommand): VerifyTwoFactorResult {

        log.info { "2FA verification attempt for session: ${command.sessionId}" }

        val process = processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.TWO_FACTOR_AUTH, command.sessionId)
            ?: throw TwoFactorSessionInvalidException("Invalid session")

        val initialRequest = process.getInitialRequest()

        val forUserId = initialRequest.getDataValueOrNull(ProcessRequestDataName.USER_IDENTIFIER)?.let { UUID.fromString(it) }
            ?: throw TwoFactorSessionInvalidException("There is a problem with this session, please try again")

        val storedDeviceFingerprint = initialRequest.getDataValueOrNull(ProcessRequestDataName.DEVICE_FINGERPRINT)

        if (storedDeviceFingerprint != null) {
            val currentFingerprint = command.httpRequest?.let {
                deviceFingerprintService.generateFingerprintFromRequest(it, command.ipAddress)
            } ?: deviceFingerprintService.generateFingerprint(command.userAgent, command.ipAddress)

            if (currentFingerprint != storedDeviceFingerprint) {
                log.warn { "Device fingerprint mismatch for 2FA session ${command.sessionId}" }
                throw TwoFactorSessionInvalidException(
                    messageService.getMessage("twofactor.error.device_mismatch")
                )
            }
        }

        val twoFactorMethod = initialRequest.getDataValueOrNull(ProcessRequestDataName.TWO_FACTOR_METHOD) ?: "EMAIL"

        val user = userRepository.findById(forUserId).orElseThrow {
            TwoFactorSessionInvalidException("There is a problem with this session, please try again")
        }

        accountLockoutService.checkAccountLockStatus(user)

        val codeValid = when (twoFactorMethod) {
            "TOTP" -> {
                val secret = user.totpSecret
                when {
                    secret != null && totpService.verifyCode(secret, command.code) -> true
                    else -> verifyRecoveryCode(forUserId, command.code)
                }
            }
            else -> {
                val storedCode = process.requests.maxByOrNull { it.id }?.getDataValueOrNull(ProcessRequestDataName.AUTHENTICATION_REFERENCE)
                    ?: initialRequest.getDataValueOrNull(ProcessRequestDataName.AUTHENTICATION_REFERENCE)
                    ?: throw TwoFactorSessionInvalidException("There is a problem with this session, please try again")
                storedCode == command.code
            }
        }

        if (!codeValid) {
            accountLockoutService.handleFailedLogin(user.username)
            log.warn { "Invalid 2FA code provided (method=$twoFactorMethod)" }
            throw TwoFactorCodeInvalidException("Invalid verification code")
        }

        accountLockoutService.handleSuccessfulLogin(user.username)

        user.twoFactorLastVerified = Instant.now()

        processGateway.completeProcess(process.publicId, initialRequest.id)

        val userDetails = CustomUserDetails(user)
        val accessToken = jwtTokenService.generateToken(user, userDetails)
        val refreshToken = refreshTokenService.createRefreshToken(
            user = user,
            ipAddress = command.ipAddress,
            userAgent = command.userAgent,
            deviceFingerprint = storedDeviceFingerprint
        )

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = user.id.toString(),
                actorName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.email?.value ?: "" },
                merchantId = user.merchantId?.toString() ?: "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "Two-factor authentication completed successfully",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf(
                    PROCESS_ID.value to process.publicId.toString(),
                    USERNAME.value to user.username,
                    SESSION_ID.value to command.sessionId,
                    CODE.value to command.code,
                    USER_ID.value to user.id.toString()
                )
            )
        )

        log.info { "2FA verification successful for user ${user.username}" }

        // Automatically trust device if enabled and device fingerprint is available
        if (trustedDeviceEnabled && storedDeviceFingerprint != null) {
            try {
                val trustResult = pipeline.send(
                    TrustDeviceCommand(
                        userId = user.id!!,
                        sessionId = command.sessionId,
                        deviceFingerprint = storedDeviceFingerprint,
                        deviceName = command.deviceName,
                        ipAddress = command.ipAddress,
                        userAgent = command.userAgent,
                        trustDurationDays = defaultTrustDurationDays
                    )
                )

                log.info { "Device automatically trusted for user ${user.username}: ${trustResult.message}" }
            } catch (e: Exception) {
                log.error(e) { "Failed to trust device for user ${user.username}" }
            }
        }

        return VerifyTwoFactorResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtTokenService.getTokenExpirySeconds(),
            user = TwoFactorUserInfo(
                username = user.username,
                name = userDetails.getFullName(),
                email = user.email?.value ?: ""
            ),
            message = "Verification successful"
        )
    }

    private fun verifyRecoveryCode(userId: UUID, code: String): Boolean {
        val unusedCodes = recoveryCodeRepository.findByUserIdAndUsedFalse(userId)
        val matchedCode = unusedCodes.firstOrNull { totpService.verifyRecoveryCode(code, it.codeHash) }
            ?: return false

        matchedCode.used = true
        matchedCode.usedAt = java.time.Instant.now()
        recoveryCodeRepository.save(matchedCode)

        log.info { "Recovery code used for user $userId, remaining: ${unusedCodes.size - 1}" }
        return true
    }
}