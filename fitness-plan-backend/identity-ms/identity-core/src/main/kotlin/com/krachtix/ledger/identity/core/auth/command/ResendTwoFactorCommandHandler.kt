package com.krachtix.identity.core.auth.command

import com.krachtix.identity.commons.audit.AuditEvent
import com.krachtix.identity.commons.audit.AuditResource
import com.krachtix.identity.commons.audit.IdentityType
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.TwoFactorSessionInvalidException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.MakeProcessRequestPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.TokenGenerationUtility
import com.krachtix.identity.core.auth.service.TwoFactorEmailService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class ResendTwoFactorCommandHandler(
    private val processGateway: ProcessGateway,
    private val userRepository: OAuthUserRepository,
    private val twoFactorEmailService: TwoFactorEmailService,
    private val tokenGenerationUtility: TokenGenerationUtility,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val messageService: MessageService,
    @Value("\${identity.2fa.token-length:6}") private val tokenLength: Int = 6,
    @Value("\${identity.2fa.min-interval-seconds:60}") private val minIntervalSeconds: Long = 60,
    @Value("\${identity.2fa.max-codes-per-window:5}") private val maxCodesPerWindow: Int = 5,
    @Value("\${identity.2fa.window-seconds:1800}") private val windowSeconds: Long = 1800
) : Command.Handler<ResendTwoFactorCommand, ResendTwoFactorResult> {

    override fun handle(command: ResendTwoFactorCommand): ResendTwoFactorResult {
        log.info { "2FA resend attempt for session: ${command.sessionId}" }

        val process =
            processGateway.findPendingProcessByTypeAndExternalReference(ProcessType.TWO_FACTOR_AUTH, command.sessionId)
                ?: throw TwoFactorSessionInvalidException("Invalid session")

        if (process.state != ProcessState.PENDING) {
            log.warn { "Cannot resend 2FA code for process in state: ${process.state}" }
            throw TwoFactorSessionInvalidException("Session is no longer valid")
        }

        val twoFactorMethod = process.getInitialRequest().getDataValueOrNull(ProcessRequestDataName.TWO_FACTOR_METHOD) ?: "EMAIL"
        if (twoFactorMethod == "TOTP") {
            throw InvalidRequestException(messageService.getMessage("twofactor.totp.resend_not_applicable"))
        }

        val userId = UUID.fromString(process.getInitialRequest().getStakeholderValue(ProcessStakeholderType.FOR_USER))

        val recentProcessesInWindow = processGateway.findRecentPendingProcessesByTypeAndForUserId(
            ProcessType.TWO_FACTOR_AUTH,
            userId,
            Instant.now().minusSeconds(windowSeconds)
        )

        val totalResendCount = recentProcessesInWindow.sumOf { it.requests.size }
        if (totalResendCount >= maxCodesPerWindow) {
            log.warn { "Rate limit exceeded for user $userId: $totalResendCount codes in last $windowSeconds seconds" }
            throw InvalidRequestException(messageService.getMessage("twofactor.error.rate_limit_exceeded"))
        }

        val transitions = processGateway.getProcessTransitions(process.publicId)
        val lastResendTransition = transitions
            .filter { it.event == ProcessEvent.AUTH_TOKEN_RESEND }
            .maxByOrNull { it.timestamp }

        val lastCodeTime = lastResendTransition?.timestamp ?: process.createdDate
        val secondsSinceLastCode = Duration.between(lastCodeTime, Instant.now()).seconds
        if (secondsSinceLastCode < minIntervalSeconds) {
            log.info { "Rate limit: user $userId must wait ${minIntervalSeconds - secondsSinceLastCode} more seconds" }
            throw InvalidRequestException(messageService.getMessage("twofactor.error.rate_limit"))
        }

        val user = userRepository.findById(userId).orElseThrow {
            IllegalStateException("User not found")
        }

        val newCode = tokenGenerationUtility.generateNumericToken(tokenLength)

        processGateway.makeRequest(
            MakeProcessRequestPayload(
                userId,
                process.publicId,
                ProcessEvent.AUTH_TOKEN_RESEND,
                ProcessRequestType.RESEND_AUTHENTICATION,
                ProcessChannel.BUSINESS_WEB,
                data = mapOf(ProcessRequestDataName.AUTHENTICATION_REFERENCE to newCode)
            )
        )

        twoFactorEmailService.sendTwoFactorCode(user, newCode, command.ipAddress)

        applicationEventPublisher.publishEvent(
            AuditEvent(
                actorId = user.id.toString(),
                actorName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.email?.value ?: "" },
                merchantId = user.merchantId?.toString() ?: "unknown",
                identityType = IdentityType.USER,
                resource = AuditResource.IDENTITY,
                event = "Two-factor authentication code resent",
                eventTime = Instant.now(),
                timeRecorded = Instant.now(),
                payload = mapOf<String, String>(
                    "processId" to process.publicId.toString(),
                    "username" to user.username,
                    "sessionId" to command.sessionId,
                    "ipAddress" to (command.ipAddress ?: "unknown"),
                    "newCode" to newCode,
                    "userId" to user.id.toString()
                )
            )
        )

        log.info { "2FA code resent to user ${user.username}" }

        return ResendTwoFactorResult(
            sessionId = command.sessionId,
            message = "New verification code sent!"
        )
    }
}