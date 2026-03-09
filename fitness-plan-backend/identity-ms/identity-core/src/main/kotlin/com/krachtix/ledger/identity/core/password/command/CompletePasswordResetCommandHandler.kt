package com.krachtix.identity.core.password.command


import com.krachtix.identity.commons.audit.AuditEvent
import com.krachtix.identity.commons.audit.AuditPayloadKey.PROCESS_ID
import com.krachtix.identity.commons.audit.AuditPayloadKey.REFERENCE
import com.krachtix.identity.commons.audit.AuditPayloadKey.TOKEN
import com.krachtix.identity.commons.audit.AuditPayloadKey.USERNAME
import com.krachtix.identity.commons.audit.AuditPayloadKey.USER_ID
import com.krachtix.identity.commons.audit.AuditResource
import com.krachtix.identity.commons.audit.IdentityType
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.process.MakeProcessRequestPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.commons.process.enumeration.ProcessEvent
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.identity.core.auth.service.UserTokenRevocationService
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import com.krachtix.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class CompletePasswordResetCommandHandler(
    private val processGateway: ProcessGateway,
    private val userRepository: OAuthUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val refreshTokenService: RefreshTokenService,
    private val userTokenRevocationService: UserTokenRevocationService
) : Command.Handler<CompletePasswordResetCommand, CompletePasswordResetResult> {

    override fun handle(command: CompletePasswordResetCommand): CompletePasswordResetResult {
        log.info { "Completing password reset for reference: ${command.reference}" }

        val newPasswordStr = String(command.newPassword)
        try {
            val processId = UUID.fromString(command.reference)

            val process = processGateway.findPendingProcessByPublicId(
                id = processId
            ) ?: throw InvalidRequestException("Invalid or expired password reset reference")

            if (process.externalReference != command.token) {
                throw InvalidRequestException("Invalid password reset token")
            }

            val initialRequest = process.getInitialRequest()

            val userId = initialRequest.getDataValueOrNull(ProcessRequestDataName.USER_IDENTIFIER)?.let { UUID.fromString(it) }
                ?: throw InvalidRequestException("User ID not found in process")

            val user = userRepository.findById(userId).orElse(null)
                ?: throw RecordNotFoundException("User not found with ID: $userId")

            user.password = passwordEncoder.encode(newPasswordStr)!!
            user.resetFailedLoginAttempts()
            userRepository.save(user)

            refreshTokenService.revokeAllUserTokens(userId)
            userTokenRevocationService.revokeUserAccessTokens(userId)

            val completeRequest = MakeProcessRequestPayload(
                userId = userId,
                publicId = processId,
                eventType = ProcessEvent.PROCESS_COMPLETED,
                requestType = ProcessRequestType.COMPLETE_PROCESS,
                channel = ProcessChannel.BUSINESS_WEB
            )

            processGateway.makeRequest(completeRequest)

            applicationEventPublisher.publishEvent(
                AuditEvent(
                    actorId = user.id.toString(),
                    actorName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.email?.value ?: "" },
                    merchantId = user.merchantId?.toString() ?: "unknown",
                    identityType = IdentityType.USER,
                    resource = AuditResource.IDENTITY,
                    event = "Password reset completed",
                    eventTime = Instant.now(),
                    timeRecorded = Instant.now(),
                    payload = mapOf(
                        PROCESS_ID.value to processId.toString(),
                        USERNAME.value to user.username,
                        REFERENCE.value to command.reference,
                        TOKEN.value to command.token,
                        USER_ID.value to userId.toString()
                    )
                )
            )

            log.info { "Password reset completed successfully for user: ${user.username}" }

            return CompletePasswordResetResult(
                success = true,
                message = "Password reset completed successfully",
                userId = userId
            )
        } finally {
            command.newPassword.fill('\u0000')
        }
    }
}