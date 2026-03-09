package com.krachtix.identity.core.profile.command

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessStakeholderType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.auth.service.UserTokenRevocationService
import com.krachtix.identity.core.entity.RegistrationSource
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@Transactional
class ChangePasswordCommandHandler(
    private val userService: UserService,
    private val userRepository: OAuthUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val processGateway: ProcessGateway,
    private val messageService: MessageService,
    private val refreshTokenService: RefreshTokenService,
    private val userTokenRevocationService: UserTokenRevocationService
) : Command.Handler<ChangePasswordCommand, ChangePasswordResult> {

    override fun handle(command: ChangePasswordCommand): ChangePasswordResult {
        val user = userService.getCurrentUser()

        try {
            if (user.registrationSource == RegistrationSource.OAUTH_GOOGLE ||
                user.registrationSource == RegistrationSource.OAUTH_MICROSOFT
            ) {
                throw InvalidRequestException(messageService.getMessage("auth.password.oauth_user_cannot_change"))
            }

            val currentPasswordStr = String(command.currentPassword)
            val newPasswordStr = String(command.newPassword)
            if (!passwordEncoder.matches(currentPasswordStr, user.password)) {
                throw InvalidRequestException(messageService.getMessage("auth.password.current_invalid"))
            }

            if (passwordEncoder.matches(newPasswordStr, user.password)) {
                throw InvalidRequestException(messageService.getMessage("auth.password.same_as_current"))
            }

            val processId = UUID.randomUUID()
            processGateway.createProcess(
                CreateNewProcessPayload(
                    userId = user.id!!,
                    publicId = processId,
                    type = ProcessType.PASSWORD_CHANGE,
                    description = "Password change for ${user.email?.value}",
                    initialState = ProcessState.PENDING,
                    requestState = ProcessState.COMPLETE,
                    channel = ProcessChannel.BUSINESS_WEB,
                    data = mapOf(
                        ProcessRequestDataName.USER_IDENTIFIER to user.id.toString()
                    ),
                    stakeholders = mapOf(
                        ProcessStakeholderType.FOR_USER to user.id.toString()
                    )
                )
            )

            user.password = passwordEncoder.encode(newPasswordStr)!!
            userRepository.save(user)

            refreshTokenService.revokeAllUserTokens(user.id!!)
            userTokenRevocationService.revokeUserAccessTokens(user.id!!)

            processGateway.findPendingProcessByPublicId(processId)?.let {
                processGateway.completeProcess(it.publicId, it.getInitialRequest().id)
            }

            log.info { "Password changed for user ${user.username}, all tokens revoked" }

            return ChangePasswordResult(
                success = true,
                message = messageService.getMessage("auth.password.change_success")
            )
        } finally {
            command.currentPassword.fill('\u0000')
            command.newPassword.fill('\u0000')
        }
    }
}
