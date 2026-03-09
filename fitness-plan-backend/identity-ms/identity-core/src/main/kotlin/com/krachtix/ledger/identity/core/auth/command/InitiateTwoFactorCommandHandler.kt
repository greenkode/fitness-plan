package com.krachtix.identity.core.auth.command

import com.krachtix.identity.commons.audit.AuditEvent
import com.krachtix.identity.commons.audit.AuditPayloadKey.IP_ADDRESS
import com.krachtix.identity.commons.audit.AuditPayloadKey.SESSION_ID
import com.krachtix.identity.commons.audit.AuditPayloadKey.TRUSTED_DEVICE_ID
import com.krachtix.identity.commons.audit.AuditPayloadKey.USERNAME
import com.krachtix.identity.commons.audit.AuditPayloadKey.USER_ID
import com.krachtix.identity.commons.audit.AuditResource
import com.krachtix.identity.commons.audit.IdentityType
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.EmailNotVerifiedException
import com.krachtix.commons.exception.ProcessServiceException
import com.krachtix.commons.exception.TwoFactorAuthenticationRequiredException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.core.auth.dto.DirectLoginResult
import com.krachtix.identity.core.auth.service.JwtTokenService
import com.krachtix.identity.core.auth.service.TwoFactorEmailService
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.AccountLockoutService
import com.krachtix.identity.core.service.CustomUserDetails
import com.krachtix.identity.core.service.TokenGenerationUtility
import com.krachtix.identity.core.trusteddevice.service.DeviceFingerprintService
import com.krachtix.identity.core.trusteddevice.service.TrustedDeviceService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class InitiateTwoFactorCommandHandler(
    private val authenticationManager: AuthenticationManager,
    private val processGateway: ProcessGateway,
    private val accountLockoutService: AccountLockoutService,
    private val twoFactorEmailService: TwoFactorEmailService,
    private val tokenGenerationUtility: TokenGenerationUtility,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val trustedDeviceService: TrustedDeviceService,
    private val deviceFingerprintService: DeviceFingerprintService,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val userRepository: OAuthUserRepository,
    private val messageService: MessageService,
    @Value("\${identity.2fa.token-length:6}") private val tokenLength: Int = 6,
    @Value("\${identity.2fa.skip-for-trusted-devices:true}") private val skipForTrustedDevices: Boolean = true,
    @Value("\${identity.2fa.min-interval-seconds:60}") private val minIntervalSeconds: Long = 60,
    @Value("\${identity.2fa.max-codes-per-window:5}") private val maxCodesPerWindow: Int = 5,
    @Value("\${identity.2fa.window-seconds:1800}") private val windowSeconds: Long = 1800,
    @Value("\${identity.2fa.reverification-interval-hours:24}") private val reverificationIntervalHours: Long = 24
) : Command.Handler<InitiateTwoFactorCommand, DirectLoginResult> {

    override fun handle(command: InitiateTwoFactorCommand): DirectLoginResult {
        log.info { "2FA login attempt for user: ${command.username}" }

        val passwordStr = String(command.password)
        try {

            val authToken = UsernamePasswordAuthenticationToken(command.username, passwordStr)
            val authentication = authenticationManager.authenticate(authToken)
            val userDetails = authentication.principal as CustomUserDetails
            val oauthUser = userDetails.getOAuthUser()

            accountLockoutService.handleSuccessfulLogin(command.username)

            if (!oauthUser.emailVerified)
                throw EmailNotVerifiedException("Email not verified. Please verify your email before logging in.")

            // Generate device fingerprint
            val deviceFingerprint = if (command.httpRequest != null) {
                deviceFingerprintService.generateFingerprintFromRequest(command.httpRequest, command.ipAddress)
            } else {
                command.deviceFingerprint ?: deviceFingerprintService.generateFingerprint(
                    command.userAgent,
                    command.ipAddress
                )
            }

            val userId = oauthUser.id
            if (skipForTrustedDevices && userId != null) {
                val trustedDevice = trustedDeviceService.checkTrustedDevice(userId, deviceFingerprint)

                if (trustedDevice != null) {
                    val lastVerified = oauthUser.twoFactorLastVerified
                    val needsReverification = lastVerified == null ||
                        lastVerified.isBefore(Instant.now().minus(Duration.ofHours(reverificationIntervalHours)))

                    if (!needsReverification) {
                        log.info { "User ${oauthUser.username} logged in from trusted device (within reverification window)" }

                        val accessToken = jwtTokenService.generateToken(oauthUser, userDetails)
                        val refreshToken = refreshTokenService.createRefreshToken(
                            user = oauthUser,
                            ipAddress = command.ipAddress,
                            userAgent = command.userAgent,
                            deviceFingerprint = deviceFingerprint
                        )

                        applicationEventPublisher.publishEvent(
                            AuditEvent(
                                actorId = userId.toString(),
                                actorName = "${oauthUser.firstName ?: ""} ${oauthUser.lastName ?: ""}".trim()
                                    .ifEmpty { oauthUser.email?.value ?: "" },
                                merchantId = oauthUser.merchantId?.toString() ?: "unknown",
                                identityType = IdentityType.USER,
                                resource = AuditResource.IDENTITY,
                                event = "Direct login from trusted device",
                                eventTime = Instant.now(),
                                timeRecorded = Instant.now(),
                                payload = mapOf(
                                    USERNAME.value to oauthUser.username,
                                    IP_ADDRESS.value to (command.ipAddress ?: "unknown"),
                                    TRUSTED_DEVICE_ID.value to (trustedDevice.id?.toString() ?: "unknown")
                                )
                            )
                        )

                        return DirectLoginResult(
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            expiresIn = jwtTokenService.getTokenExpirySeconds(),
                            trustedDeviceId = trustedDevice.id?.toString(),
                            trustedUntil = trustedDevice.expiresAt
                        )
                    }

                    log.info { "Trusted device detected but reverification interval exceeded for user ${oauthUser.username}" }
                }
            }

            if (oauthUser.totpEnabled) {
                val sessionId = RandomStringUtils.secure().nextAlphanumeric(100)

                processGateway.createProcess(
                    CreateNewProcessPayload(
                        userId = userId!!,
                        publicId = UUID.randomUUID(),
                        type = ProcessType.TWO_FACTOR_AUTH,
                        description = "TOTP Two Factor Authentication for ${oauthUser.email?.value}",
                        initialState = ProcessState.PENDING,
                        requestState = ProcessState.COMPLETE,
                        channel = ProcessChannel.BUSINESS_WEB,
                        externalReference = sessionId,
                        data = mapOf(
                            ProcessRequestDataName.USER_IDENTIFIER to userId.toString(),
                            ProcessRequestDataName.DEVICE_FINGERPRINT to deviceFingerprint,
                            ProcessRequestDataName.TWO_FACTOR_METHOD to "TOTP"
                        ),
                        stakeholders = mapOf(
                            ProcessStakeholderType.FOR_USER to userId.toString()
                        )
                    )
                )

                log.info { "TOTP 2FA required for user ${oauthUser.username}" }

                val restrictedToken = jwtTokenService.generateRestrictedTwoFactorToken(oauthUser)

                throw TwoFactorAuthenticationRequiredException(
                    sessionId = sessionId,
                    message = messageService.getMessage("twofactor.totp.required"),
                    twoFactorMethod = "TOTP",
                    restrictedToken = restrictedToken
                )
            }

            val sessionId = RandomStringUtils.secure().nextAlphanumeric(100)
            val twoFactorCode = tokenGenerationUtility.generateNumericToken(tokenLength)

            val recentProcessesInWindow = processGateway.findRecentPendingProcessesByTypeAndForUserId(
                ProcessType.TWO_FACTOR_AUTH,
                userId!!,
                Instant.now().minusSeconds(windowSeconds)
            )

            if (recentProcessesInWindow.size >= maxCodesPerWindow) {
                log.warn { "Rate limit exceeded for user ${oauthUser.username}: ${recentProcessesInWindow.size} codes in last $windowSeconds seconds" }
                throw InvalidRequestException(messageService.getMessage("twofactor.error.rate_limit_exceeded"))
            }

            val mostRecentProcess = recentProcessesInWindow.maxByOrNull { it.createdDate }
            if (mostRecentProcess != null) {
                val secondsSinceLastCode = java.time.Duration.between(mostRecentProcess.createdDate, Instant.now()).seconds
                if (secondsSinceLastCode < minIntervalSeconds) {
                    log.info { "Rate limit: user ${oauthUser.username} must wait ${minIntervalSeconds - secondsSinceLastCode} more seconds" }
                    throw InvalidRequestException(messageService.getMessage("twofactor.error.rate_limit"))
                }

                processGateway.failProcess(mostRecentProcess.publicId)
                log.info { "Cancelled existing 2FA process ${mostRecentProcess.publicId} for user ${oauthUser.username}" }
            }

            processGateway.createProcess(
                CreateNewProcessPayload(
                    userId = userId,
                    publicId = UUID.randomUUID(),
                    type = ProcessType.TWO_FACTOR_AUTH,
                    description = "Two Factor Authentication for ${oauthUser.email?.value}",
                    initialState = ProcessState.PENDING,
                    requestState = ProcessState.COMPLETE,
                    channel = ProcessChannel.BUSINESS_WEB,
                    externalReference = sessionId,
                    data = mapOf(
                        ProcessRequestDataName.USER_IDENTIFIER to userId.toString(),
                        ProcessRequestDataName.AUTHENTICATION_REFERENCE to twoFactorCode,
                        ProcessRequestDataName.DEVICE_FINGERPRINT to deviceFingerprint
                    ),
                    stakeholders = mapOf(
                        ProcessStakeholderType.FOR_USER to userId.toString()
                    )
                )
            )

            twoFactorEmailService.sendTwoFactorCode(oauthUser, twoFactorCode, command.ipAddress)

            applicationEventPublisher.publishEvent(
                AuditEvent(
                    actorId = userId.toString(),
                    actorName = "${oauthUser.firstName ?: ""} ${oauthUser.lastName ?: ""}".trim().ifEmpty { oauthUser.email?.value ?: "" },
                    merchantId = oauthUser.merchantId?.toString() ?: "unknown",
                    identityType = IdentityType.USER,
                    resource = AuditResource.IDENTITY,
                    event = "Two-factor authentication initiated",
                    eventTime = Instant.now(),
                    timeRecorded = Instant.now(),
                    payload = mapOf(
                        USERNAME.value to oauthUser.username,
                        SESSION_ID.value to sessionId,
                        IP_ADDRESS.value to (command.ipAddress ?: "unknown"),
                        USER_ID.value to userId.toString()
                    )
                )
            )

            log.info { "2FA code sent to user ${oauthUser.username}" }

            val restrictedToken = jwtTokenService.generateRestrictedTwoFactorToken(oauthUser)

            throw TwoFactorAuthenticationRequiredException(
                sessionId = sessionId,
                message = "Verification code sent!",
                restrictedToken = restrictedToken
            )


        } catch (e: TwoFactorAuthenticationRequiredException) {
            throw e
        } catch (e: InvalidRequestException) {
            throw e
        } catch (e: EmailNotVerifiedException) {
            throw e
        } catch (e: AuthenticationException) {
            log.error(e) { "Failed 2FA login attempt for user: ${command.username}" }
            accountLockoutService.handleFailedLogin(command.username)
            throw InvalidRequestException("Invalid credentials")
        } catch (e: Exception) {
            log.error(e) { "Error during 2FA login for user: ${command.username}" }
            throw ProcessServiceException("An error occurred during login")
        } finally {
            command.password.fill('\u0000')
        }
    }

}