package com.krachtix.identity.core.invitation.command

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.process.CreateNewProcessPayload
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.ProcessRequestDto
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.audit.AuditEvent
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.deletion.service.UserDeletionResult
import com.krachtix.identity.core.deletion.service.UserDeletionService
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.repository.OAuthUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RevokeInvitationCommandHandlerTest {

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var userDeletionService: UserDeletionService

    @Mock
    private lateinit var messageService: MessageService

    @Mock
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @InjectMocks
    private lateinit var handler: RevokeInvitationCommandHandler

    @Captor
    private lateinit var processPayloadCaptor: ArgumentCaptor<CreateNewProcessPayload>

    @Captor
    private lateinit var auditEventCaptor: ArgumentCaptor<AuditEvent>

    private val merchantId = UUID.randomUUID()
    private val revokingUserId = UUID.randomUUID()
    private val targetUserId = UUID.randomUUID()

    private lateinit var revokingUser: OAuthUser
    private lateinit var targetUser: OAuthUser

    @BeforeEach
    fun setUp() {
        revokingUser = OAuthUser(
            username = "admin@company.com",
            password = "encoded-password",
            email = Email("admin@company.com")
        ).apply {
            id = revokingUserId
            firstName = "Admin"
            lastName = "User"
            merchantId = this@RevokeInvitationCommandHandlerTest.merchantId
            invitationStatus = true
        }

        targetUser = OAuthUser(
            username = "pending@company.com",
            password = "encoded-password",
            email = Email("pending@company.com")
        ).apply {
            id = targetUserId
            merchantId = this@RevokeInvitationCommandHandlerTest.merchantId
            invitationStatus = false
        }
    }

    private fun createProcessDto(publicId: UUID = UUID.randomUUID()): ProcessDto {
        val initialRequest = ProcessRequestDto(
            id = 1L,
            type = ProcessRequestType.CREATE_NEW_PROCESS,
            state = ProcessState.COMPLETE,
            stakeholders = emptyMap(),
            data = emptyMap()
        )
        return ProcessDto(
            id = 1L,
            publicId = publicId,
            state = ProcessState.PENDING,
            type = ProcessType.INVITATION_REVOCATION,
            channel = ProcessChannel.BUSINESS_WEB,
            createdDate = Instant.now(),
            requests = listOf(initialRequest)
        )
    }

    @Nested
    inner class SuccessfulRevocation {

        @Test
        fun `should revoke pending invitation successfully`() {
            val processDto = createProcessDto()
            val invitationProcessDto = createProcessDto().copy(type = ProcessType.MERCHANT_USER_INVITATION)

            whenever(userRepository.findById(revokingUserId)).thenReturn(Optional.of(revokingUser))
            whenever(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser))
            whenever(processGateway.createProcess(any())).thenReturn(processDto)
            whenever(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.MERCHANT_USER_INVITATION, targetUserId))
                .thenReturn(invitationProcessDto)
            whenever(userDeletionService.deleteUser(targetUserId))
                .thenReturn(UserDeletionResult(success = true, message = "deleted", userId = targetUserId))
            whenever(messageService.getMessage("invitation.success.revoked")).thenReturn("Invitation revoked")

            val result = handler.handle(RevokeInvitationCommand(targetUserId, revokingUserId))

            assertThat(result.success).isTrue()
            assertThat(result.message).isEqualTo("Invitation revoked")
        }

        @Test
        fun `should create revocation process before deletion`() {
            val processDto = createProcessDto()

            whenever(userRepository.findById(revokingUserId)).thenReturn(Optional.of(revokingUser))
            whenever(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser))
            whenever(processGateway.createProcess(capture(processPayloadCaptor))).thenReturn(processDto)
            whenever(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.MERCHANT_USER_INVITATION, targetUserId))
                .thenReturn(null)
            whenever(userDeletionService.deleteUser(targetUserId))
                .thenReturn(UserDeletionResult(success = true, message = "deleted", userId = targetUserId))
            whenever(messageService.getMessage("invitation.success.revoked")).thenReturn("Invitation revoked")

            handler.handle(RevokeInvitationCommand(targetUserId, revokingUserId))

            val payload = processPayloadCaptor.value
            assertThat(payload.userId).isEqualTo(revokingUserId)
            assertThat(payload.type).isEqualTo(ProcessType.INVITATION_REVOCATION)
            assertThat(payload.initialState).isEqualTo(ProcessState.PENDING)
            assertThat(payload.requestState).isEqualTo(ProcessState.COMPLETE)
            assertThat(payload.channel).isEqualTo(ProcessChannel.BUSINESS_WEB)
        }

        @Test
        fun `should fail pending invitation process when it exists`() {
            val processDto = createProcessDto()
            val invitationPublicId = UUID.randomUUID()
            val invitationProcessDto = createProcessDto(invitationPublicId).copy(type = ProcessType.MERCHANT_USER_INVITATION)

            whenever(userRepository.findById(revokingUserId)).thenReturn(Optional.of(revokingUser))
            whenever(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser))
            whenever(processGateway.createProcess(any())).thenReturn(processDto)
            whenever(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.MERCHANT_USER_INVITATION, targetUserId))
                .thenReturn(invitationProcessDto)
            whenever(userDeletionService.deleteUser(targetUserId))
                .thenReturn(UserDeletionResult(success = true, message = "deleted", userId = targetUserId))
            whenever(messageService.getMessage("invitation.success.revoked")).thenReturn("Invitation revoked")

            handler.handle(RevokeInvitationCommand(targetUserId, revokingUserId))

            verify(processGateway).failProcess(invitationPublicId)
        }

        @Test
        fun `should handle missing pending invitation process gracefully`() {
            val processDto = createProcessDto()

            whenever(userRepository.findById(revokingUserId)).thenReturn(Optional.of(revokingUser))
            whenever(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser))
            whenever(processGateway.createProcess(any())).thenReturn(processDto)
            whenever(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.MERCHANT_USER_INVITATION, targetUserId))
                .thenReturn(null)
            whenever(userDeletionService.deleteUser(targetUserId))
                .thenReturn(UserDeletionResult(success = true, message = "deleted", userId = targetUserId))
            whenever(messageService.getMessage("invitation.success.revoked")).thenReturn("Invitation revoked")

            val result = handler.handle(RevokeInvitationCommand(targetUserId, revokingUserId))

            assertThat(result.success).isTrue()
            verify(processGateway, never()).failProcess(any())
        }

        @Test
        fun `should delete user via deletion service`() {
            val processDto = createProcessDto()

            whenever(userRepository.findById(revokingUserId)).thenReturn(Optional.of(revokingUser))
            whenever(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser))
            whenever(processGateway.createProcess(any())).thenReturn(processDto)
            whenever(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.MERCHANT_USER_INVITATION, targetUserId))
                .thenReturn(null)
            whenever(userDeletionService.deleteUser(targetUserId))
                .thenReturn(UserDeletionResult(success = true, message = "deleted", userId = targetUserId))
            whenever(messageService.getMessage("invitation.success.revoked")).thenReturn("Invitation revoked")

            handler.handle(RevokeInvitationCommand(targetUserId, revokingUserId))

            verify(userDeletionService).deleteUser(targetUserId)
        }

        @Test
        fun `should publish audit event with pre-deletion email`() {
            val processDto = createProcessDto()

            whenever(userRepository.findById(revokingUserId)).thenReturn(Optional.of(revokingUser))
            whenever(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser))
            whenever(processGateway.createProcess(any())).thenReturn(processDto)
            whenever(processGateway.findLatestPendingProcessesByTypeAndForUserId(ProcessType.MERCHANT_USER_INVITATION, targetUserId))
                .thenReturn(null)
            whenever(userDeletionService.deleteUser(targetUserId))
                .thenReturn(UserDeletionResult(success = true, message = "deleted", userId = targetUserId))
            whenever(messageService.getMessage("invitation.success.revoked")).thenReturn("Invitation revoked")

            handler.handle(RevokeInvitationCommand(targetUserId, revokingUserId))

            verify(applicationEventPublisher).publishEvent(capture(auditEventCaptor))
            val auditEvent = auditEventCaptor.value
            assertThat(auditEvent.actorId).isEqualTo(revokingUserId.toString())
            assertThat(auditEvent.actorName).isEqualTo("Admin User")
            assertThat(auditEvent.merchantId).isEqualTo(merchantId.toString())
            assertThat(auditEvent.event).isEqualTo("Invitation revoked for pending@company.com")
        }
    }

    @Nested
    inner class ValidationErrors {

        @Test
        fun `should throw when target user not found`() {
            whenever(userRepository.findById(revokingUserId)).thenReturn(Optional.of(revokingUser))
            whenever(userRepository.findById(targetUserId)).thenReturn(Optional.empty())
            whenever(messageService.getMessage("invitation.error.user_not_found")).thenReturn("User not found")

            assertThatThrownBy { handler.handle(RevokeInvitationCommand(targetUserId, revokingUserId)) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("User not found")

            verify(userDeletionService, never()).deleteUser(any())
        }

        @Test
        fun `should throw when revoking user not found`() {
            whenever(userRepository.findById(revokingUserId)).thenReturn(Optional.empty())
            whenever(messageService.getMessage("invitation.error.user_not_found")).thenReturn("User not found")

            assertThatThrownBy { handler.handle(RevokeInvitationCommand(targetUserId, revokingUserId)) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("User not found")

            verify(userDeletionService, never()).deleteUser(any())
        }

        @Test
        fun `should throw when revoking self`() {
            whenever(userRepository.findById(revokingUserId)).thenReturn(Optional.of(revokingUser))
            whenever(messageService.getMessage("invitation.error.cannot_revoke_self")).thenReturn("Cannot revoke self")

            assertThatThrownBy { handler.handle(RevokeInvitationCommand(revokingUserId, revokingUserId)) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("Cannot revoke self")

            verify(userDeletionService, never()).deleteUser(any())
        }

        @Test
        fun `should throw when target user is already active`() {
            targetUser.invitationStatus = true

            whenever(userRepository.findById(revokingUserId)).thenReturn(Optional.of(revokingUser))
            whenever(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser))
            whenever(messageService.getMessage("invitation.error.user_already_active")).thenReturn("User already active")

            assertThatThrownBy { handler.handle(RevokeInvitationCommand(targetUserId, revokingUserId)) }
                .isInstanceOf(InvalidRequestException::class.java)
                .hasMessage("User already active")

            verify(userDeletionService, never()).deleteUser(any())
        }

        @Test
        fun `should throw when target user belongs to different merchant`() {
            targetUser.merchantId = UUID.randomUUID()

            whenever(userRepository.findById(revokingUserId)).thenReturn(Optional.of(revokingUser))
            whenever(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser))
            whenever(messageService.getMessage("invitation.error.user_not_found")).thenReturn("User not found")

            assertThatThrownBy { handler.handle(RevokeInvitationCommand(targetUserId, revokingUserId)) }
                .isInstanceOf(RecordNotFoundException::class.java)
                .hasMessage("User not found")

            verify(userDeletionService, never()).deleteUser(any())
        }
    }
}
