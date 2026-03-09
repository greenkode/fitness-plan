package com.krachtix.identity.core.password.command

import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.process.ProcessChannel
import com.krachtix.commons.process.ProcessDto
import com.krachtix.commons.process.ProcessRequestDto
import com.krachtix.commons.process.enumeration.ProcessRequestDataName
import com.krachtix.commons.process.enumeration.ProcessRequestType
import com.krachtix.commons.process.enumeration.ProcessState
import com.krachtix.commons.process.enumeration.ProcessType
import com.krachtix.identity.commons.process.ProcessGateway
import com.krachtix.identity.core.auth.service.UserTokenRevocationService
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.refreshtoken.service.RefreshTokenService
import com.krachtix.identity.core.repository.OAuthUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CompletePasswordResetCommandHandlerTest {

    @Mock
    private lateinit var processGateway: ProcessGateway

    @Mock
    private lateinit var userRepository: OAuthUserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @Mock
    private lateinit var refreshTokenService: RefreshTokenService

    @Mock
    private lateinit var userTokenRevocationService: UserTokenRevocationService

    @InjectMocks
    private lateinit var handler: CompletePasswordResetCommandHandler

    private lateinit var user: OAuthUser
    private val userId = UUID.randomUUID()
    private val processPublicId = UUID.randomUUID()
    private val resetToken = "valid-reset-token"

    @BeforeEach
    fun setUp() {
        user = OAuthUser("testuser", "oldEncodedPassword").apply {
            id = userId
            merchantId = UUID.randomUUID()
            failedLoginAttempts = 3
        }
    }

    @Nested
    inner class WhenPasswordResetSucceeds {

        @Test
        fun `should successfully complete password reset`() {
            val command = CompletePasswordResetCommand(
                reference = processPublicId.toString(),
                token = resetToken,
                newPassword = "newSecurePass".toCharArray()
            )

            val processDto = createProcessDto(processPublicId, resetToken, userId)

            whenever(processGateway.findPendingProcessByPublicId(processPublicId)).thenReturn(processDto)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(passwordEncoder.encode("newSecurePass")).thenReturn("encodedNewPassword")
            whenever(userRepository.save(user)).thenReturn(user)

            val result = handler.handle(command)

            assertThat(result.success).isTrue()
            assertThat(result.userId).isEqualTo(userId)
            assertThat(user.password).isEqualTo("encodedNewPassword")
            verify(userRepository).save(user)
            verify(processGateway).makeRequest(any())
            verify(applicationEventPublisher).publishEvent(any<Any>())
        }

        @Test
        fun `should reset failed login attempts on successful password reset`() {
            val command = CompletePasswordResetCommand(
                reference = processPublicId.toString(),
                token = resetToken,
                newPassword = "newSecurePass".toCharArray()
            )

            val processDto = createProcessDto(processPublicId, resetToken, userId)

            whenever(processGateway.findPendingProcessByPublicId(processPublicId)).thenReturn(processDto)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(passwordEncoder.encode("newSecurePass")).thenReturn("encodedNewPassword")
            whenever(userRepository.save(user)).thenReturn(user)

            handler.handle(command)

            assertThat(user.failedLoginAttempts).isEqualTo(0)
        }
    }

    @Nested
    inner class WhenReferenceIsInvalid {

        @Test
        fun `should throw when process not found for reference`() {
            val command = CompletePasswordResetCommand(
                reference = processPublicId.toString(),
                token = resetToken,
                newPassword = "newSecurePass".toCharArray()
            )

            whenever(processGateway.findPendingProcessByPublicId(processPublicId)).thenReturn(null)

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(InvalidRequestException::class.java)

            verify(userRepository, never()).save(any())
            verify(refreshTokenService, never()).revokeAllUserTokens(any())
            verify(userTokenRevocationService, never()).revokeUserAccessTokens(any())
        }

        @Test
        fun `should throw when reference is not a valid UUID`() {
            val command = CompletePasswordResetCommand(
                reference = "not-a-uuid",
                token = resetToken,
                newPassword = "newSecurePass".toCharArray()
            )

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(IllegalArgumentException::class.java)

            verify(userRepository, never()).save(any())
            verify(refreshTokenService, never()).revokeAllUserTokens(any())
            verify(userTokenRevocationService, never()).revokeUserAccessTokens(any())
        }
    }

    @Nested
    inner class WhenTokenIsInvalid {

        @Test
        fun `should throw when token does not match process external reference`() {
            val command = CompletePasswordResetCommand(
                reference = processPublicId.toString(),
                token = "wrong-token",
                newPassword = "newSecurePass".toCharArray()
            )

            val processDto = createProcessDto(processPublicId, resetToken, userId)

            whenever(processGateway.findPendingProcessByPublicId(processPublicId)).thenReturn(processDto)

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(InvalidRequestException::class.java)

            verify(userRepository, never()).save(any())
            verify(refreshTokenService, never()).revokeAllUserTokens(any())
            verify(userTokenRevocationService, never()).revokeUserAccessTokens(any())
        }
    }

    @Nested
    inner class WhenUserNotFound {

        @Test
        fun `should throw when user not found by id`() {
            val command = CompletePasswordResetCommand(
                reference = processPublicId.toString(),
                token = resetToken,
                newPassword = "newSecurePass".toCharArray()
            )

            val processDto = createProcessDto(processPublicId, resetToken, userId)

            whenever(processGateway.findPendingProcessByPublicId(processPublicId)).thenReturn(processDto)
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            assertThatThrownBy { handler.handle(command) }
                .isInstanceOf(RecordNotFoundException::class.java)

            verify(userRepository, never()).save(any())
            verify(refreshTokenService, never()).revokeAllUserTokens(any())
            verify(userTokenRevocationService, never()).revokeUserAccessTokens(any())
        }
    }

    @Nested
    inner class TokenRevocation {

        @Test
        fun `should revoke all refresh tokens and access tokens on successful password reset`() {
            val command = CompletePasswordResetCommand(
                reference = processPublicId.toString(),
                token = resetToken,
                newPassword = "newSecurePass".toCharArray()
            )

            val processDto = createProcessDto(processPublicId, resetToken, userId)

            whenever(processGateway.findPendingProcessByPublicId(processPublicId)).thenReturn(processDto)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(passwordEncoder.encode("newSecurePass")).thenReturn("encodedNewPassword")
            whenever(userRepository.save(user)).thenReturn(user)

            handler.handle(command)

            verify(refreshTokenService).revokeAllUserTokens(userId)
            verify(userTokenRevocationService).revokeUserAccessTokens(userId)
        }
    }

    @Nested
    inner class PasswordCharArrayZeroing {

        @Test
        fun `should zero password char array after successful password reset`() {
            val newPasswordChars = "newSecurePass".toCharArray()
            val command = CompletePasswordResetCommand(
                reference = processPublicId.toString(),
                token = resetToken,
                newPassword = newPasswordChars
            )

            val processDto = createProcessDto(processPublicId, resetToken, userId)

            whenever(processGateway.findPendingProcessByPublicId(processPublicId)).thenReturn(processDto)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(passwordEncoder.encode("newSecurePass")).thenReturn("encodedNewPassword")
            whenever(userRepository.save(user)).thenReturn(user)

            handler.handle(command)

            assertThat(command.newPassword).containsOnly('\u0000')
        }

        @Test
        fun `should zero password char array even when exception is thrown`() {
            val newPasswordChars = "newSecurePass".toCharArray()
            val command = CompletePasswordResetCommand(
                reference = processPublicId.toString(),
                token = resetToken,
                newPassword = newPasswordChars
            )

            whenever(processGateway.findPendingProcessByPublicId(processPublicId)).thenReturn(null)

            runCatching { handler.handle(command) }

            assertThat(command.newPassword).containsOnly('\u0000')
        }

        @Test
        fun `should zero password char array when token is invalid`() {
            val newPasswordChars = "newSecurePass".toCharArray()
            val command = CompletePasswordResetCommand(
                reference = processPublicId.toString(),
                token = "wrong-token",
                newPassword = newPasswordChars
            )

            val processDto = createProcessDto(processPublicId, resetToken, userId)

            whenever(processGateway.findPendingProcessByPublicId(processPublicId)).thenReturn(processDto)

            runCatching { handler.handle(command) }

            assertThat(command.newPassword).containsOnly('\u0000')
        }

        @Test
        fun `should zero password char array when user not found`() {
            val newPasswordChars = "newSecurePass".toCharArray()
            val command = CompletePasswordResetCommand(
                reference = processPublicId.toString(),
                token = resetToken,
                newPassword = newPasswordChars
            )

            val processDto = createProcessDto(processPublicId, resetToken, userId)

            whenever(processGateway.findPendingProcessByPublicId(processPublicId)).thenReturn(processDto)
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            runCatching { handler.handle(command) }

            assertThat(command.newPassword).containsOnly('\u0000')
        }
    }

    private fun createProcessDto(publicId: UUID, externalReference: String, userId: UUID): ProcessDto {
        val requestDto = ProcessRequestDto(
            id = 1L,
            type = ProcessRequestType.CREATE_NEW_PROCESS,
            state = ProcessState.COMPLETE,
            stakeholders = emptyMap(),
            data = mapOf(ProcessRequestDataName.USER_IDENTIFIER to userId.toString())
        )

        return ProcessDto(
            id = 1L,
            publicId = publicId,
            state = ProcessState.PENDING,
            type = ProcessType.PASSWORD_RESET,
            channel = ProcessChannel.BUSINESS_WEB,
            createdDate = Instant.now(),
            requests = listOf(requestDto),
            externalReference = externalReference
        )
    }
}
