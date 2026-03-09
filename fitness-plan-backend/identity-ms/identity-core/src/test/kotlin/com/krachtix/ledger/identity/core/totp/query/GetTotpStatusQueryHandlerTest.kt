package com.krachtix.identity.core.totp.query

import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.service.UserService
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetTotpStatusQueryHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var recoveryCodeRepository: TotpRecoveryCodeRepository

    @InjectMocks
    private lateinit var handler: GetTotpStatusQueryHandler

    private lateinit var user: OAuthUser
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        user = OAuthUser("testuser", "encodedPassword").apply {
            id = userId
        }
    }

    @Nested
    inner class WhenTotpIsEnabled {

        @BeforeEach
        fun setUp() {
            user.totpEnabled = true
        }

        @Test
        fun `should return enabled status with remaining recovery codes count`() {
            `when`(userService.getCurrentUser()).thenReturn(user)
            `when`(recoveryCodeRepository.countByUserIdAndUsedFalse(userId)).thenReturn(5L)

            val result = handler.handle(GetTotpStatusQuery())

            assertThat(result.totpEnabled).isTrue()
            assertThat(result.remainingRecoveryCodes).isEqualTo(5L)
            verify(recoveryCodeRepository).countByUserIdAndUsedFalse(userId)
        }
    }

    @Nested
    inner class WhenTotpIsDisabled {

        @BeforeEach
        fun setUp() {
            user.totpEnabled = false
        }

        @Test
        fun `should return disabled status with zero recovery codes`() {
            `when`(userService.getCurrentUser()).thenReturn(user)

            val result = handler.handle(GetTotpStatusQuery())

            assertThat(result.totpEnabled).isFalse()
            assertThat(result.remainingRecoveryCodes).isEqualTo(0L)
            verify(recoveryCodeRepository, never()).countByUserIdAndUsedFalse(userId)
        }
    }

    @Nested
    inner class WhenUserNotFound {

        @Test
        fun `should throw when user not found`() {
            `when`(userService.getCurrentUser()).thenThrow(RecordNotFoundException("User not found"))

            assertThatThrownBy { handler.handle(GetTotpStatusQuery()) }
                .isInstanceOf(RecordNotFoundException::class.java)
        }
    }
}
