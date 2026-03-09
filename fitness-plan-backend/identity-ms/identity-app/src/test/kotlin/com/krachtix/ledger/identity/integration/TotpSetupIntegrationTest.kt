package com.krachtix.identity.integration

import com.krachtix.commons.dto.Email
import com.krachtix.commons.kyc.TrustLevel
import com.krachtix.identity.config.BaseIntegrationTest
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.RegistrationSource
import com.krachtix.identity.core.entity.UserType
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeEntity
import com.krachtix.identity.core.totp.domain.TotpRecoveryCodeRepository
import com.krachtix.identity.core.totp.service.TotpService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
class TotpSetupIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var totpService: TotpService

    @Autowired
    private lateinit var userRepository: OAuthUserRepository

    @Autowired
    private lateinit var recoveryCodeRepository: TotpRecoveryCodeRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var user: OAuthUser

    @BeforeEach
    fun setUpTestData() {
        user = OAuthUser(
            username = "totp-test@example.com",
            password = passwordEncoder.encode("TestPassword123!")!!,
            email = Email("totp-test@example.com")
        ).apply {
            this.firstName = "Totp"
            this.lastName = "Tester"
            this.userType = UserType.BUSINESS
            this.trustLevel = TrustLevel.TIER_ONE
            this.registrationSource = RegistrationSource.SELF_REGISTRATION
            this.enabled = true
        }
        userRepository.save(user)
    }

    @Nested
    inner class TotpLifecycle {

        @Test
        fun `should generate secret and persist on user`() {
            val secret = totpService.generateSecret()
            assertThat(secret).isNotBlank()

            user.totpSecret = secret
            userRepository.save(user)
            userRepository.flush()

            val reloadedUser = userRepository.findById(user.id!!).orElseThrow()
            assertThat(reloadedUser.totpSecret).isEqualTo(secret)
            assertThat(reloadedUser.totpEnabled).isFalse()
        }

        @Test
        fun `should generate valid QR code URI`() {
            val secret = totpService.generateSecret()
            val qrCodeUri = totpService.generateQrCodeUri(secret, user.email!!.value)

            assertThat(qrCodeUri).startsWith("otpauth://totp/")
            assertThat(qrCodeUri).contains(secret)
            assertThat(qrCodeUri).contains(user.email!!.value)
            assertThat(qrCodeUri).contains("algorithm=SHA1")
            assertThat(qrCodeUri).contains("digits=6")
        }

        @Test
        fun `should verify valid TOTP code against secret`() {
            val secret = totpService.generateSecret()
            val codeGenerator = dev.samstevens.totp.code.DefaultCodeGenerator(
                dev.samstevens.totp.code.HashingAlgorithm.SHA1, 6
            )
            val validCode = codeGenerator.generate(
                secret, Math.floorDiv(Instant.now().epochSecond, 30)
            )

            assertThat(totpService.verifyCode(secret, validCode)).isTrue()
        }

        @Test
        fun `should reject invalid TOTP code`() {
            val secret = totpService.generateSecret()
            assertThat(totpService.verifyCode(secret, "000000")).isFalse()
        }

        @Test
        fun `should enable TOTP and generate recovery codes that persist`() {
            val secret = totpService.generateSecret()
            user.totpSecret = secret
            user.totpEnabled = true
            userRepository.save(user)

            val recoveryCodes = totpService.generateRecoveryCodes()
            assertThat(recoveryCodes).hasSize(8)

            recoveryCodes.forEach { code ->
                recoveryCodeRepository.save(
                    TotpRecoveryCodeEntity(
                        user = user,
                        codeHash = totpService.hashRecoveryCode(code)
                    )
                )
            }
            recoveryCodeRepository.flush()

            val storedCodes = recoveryCodeRepository.findByUserIdAndUsedFalse(user.id!!)
            assertThat(storedCodes).hasSize(8)
            assertThat(storedCodes).allSatisfy { entity ->
                assertThat(entity.used).isFalse()
                assertThat(entity.usedAt).isNull()
                assertThat(entity.codeHash).isNotBlank()
            }

            val reloadedUser = userRepository.findById(user.id!!).orElseThrow()
            assertThat(reloadedUser.totpEnabled).isTrue()
            assertThat(reloadedUser.totpSecret).isEqualTo(secret)
        }

        @Test
        fun `should verify recovery code and mark it as used`() {
            val secret = totpService.generateSecret()
            user.totpSecret = secret
            user.totpEnabled = true
            userRepository.save(user)

            val recoveryCodes = totpService.generateRecoveryCodes()
            recoveryCodes.forEach { code ->
                recoveryCodeRepository.save(
                    TotpRecoveryCodeEntity(
                        user = user,
                        codeHash = totpService.hashRecoveryCode(code)
                    )
                )
            }
            recoveryCodeRepository.flush()

            val targetCode = recoveryCodes.first()
            val storedCodes = recoveryCodeRepository.findByUserIdAndUsedFalse(user.id!!)
            val matchingEntity = storedCodes.first { totpService.verifyRecoveryCode(targetCode, it.codeHash) }

            matchingEntity.used = true
            matchingEntity.usedAt = Instant.now()
            recoveryCodeRepository.save(matchingEntity)
            recoveryCodeRepository.flush()

            val remainingUnused = recoveryCodeRepository.countByUserIdAndUsedFalse(user.id!!)
            assertThat(remainingUnused).isEqualTo(7L)

            val reloadedEntity = recoveryCodeRepository.findById(matchingEntity.id!!).orElseThrow()
            assertThat(reloadedEntity.used).isTrue()
            assertThat(reloadedEntity.usedAt).isNotNull()
        }

        @Test
        fun `should not verify recovery code with wrong input`() {
            user.totpSecret = totpService.generateSecret()
            user.totpEnabled = true
            userRepository.save(user)

            val recoveryCodes = totpService.generateRecoveryCodes()
            recoveryCodes.forEach { code ->
                recoveryCodeRepository.save(
                    TotpRecoveryCodeEntity(
                        user = user,
                        codeHash = totpService.hashRecoveryCode(code)
                    )
                )
            }
            recoveryCodeRepository.flush()

            val storedCodes = recoveryCodeRepository.findByUserIdAndUsedFalse(user.id!!)
            val noMatch = storedCodes.any { totpService.verifyRecoveryCode("ZZZZ-ZZZZ", it.codeHash) }
            assertThat(noMatch).isFalse()
        }
    }

    @Nested
    inner class DisableTotp {

        @Test
        fun `should clear secret and delete recovery codes when TOTP is disabled`() {
            val secret = totpService.generateSecret()
            user.totpSecret = secret
            user.totpEnabled = true
            userRepository.save(user)

            val recoveryCodes = totpService.generateRecoveryCodes()
            recoveryCodes.forEach { code ->
                recoveryCodeRepository.save(
                    TotpRecoveryCodeEntity(
                        user = user,
                        codeHash = totpService.hashRecoveryCode(code)
                    )
                )
            }
            recoveryCodeRepository.flush()

            assertThat(recoveryCodeRepository.findByUserIdAndUsedFalse(user.id!!)).hasSize(8)

            user.totpSecret = null
            user.totpEnabled = false
            userRepository.save(user)
            recoveryCodeRepository.deleteByUserId(user.id!!)
            recoveryCodeRepository.flush()
            userRepository.flush()

            val reloadedUser = userRepository.findById(user.id!!).orElseThrow()
            assertThat(reloadedUser.totpSecret).isNull()
            assertThat(reloadedUser.totpEnabled).isFalse()

            val remainingCodes = recoveryCodeRepository.findByUserIdAndUsedFalse(user.id!!)
            assertThat(remainingCodes).isEmpty()
        }

        @Test
        fun `should have zero unused recovery codes after disable`() {
            val secret = totpService.generateSecret()
            user.totpSecret = secret
            user.totpEnabled = true
            userRepository.save(user)

            val recoveryCodes = totpService.generateRecoveryCodes()
            recoveryCodes.forEach { code ->
                recoveryCodeRepository.save(
                    TotpRecoveryCodeEntity(
                        user = user,
                        codeHash = totpService.hashRecoveryCode(code)
                    )
                )
            }
            recoveryCodeRepository.flush()

            recoveryCodeRepository.deleteByUserId(user.id!!)
            recoveryCodeRepository.flush()

            val count = recoveryCodeRepository.countByUserIdAndUsedFalse(user.id!!)
            assertThat(count).isEqualTo(0L)
        }
    }

    @Nested
    inner class RecoveryCodeGeneration {

        @Test
        fun `should generate recovery codes in expected format`() {
            val codes = totpService.generateRecoveryCodes()

            assertThat(codes).hasSize(8)
            codes.forEach { code ->
                assertThat(code).matches("[A-Z0-9]{4}-[A-Z0-9]{4}")
            }
        }

        @Test
        fun `should generate unique recovery codes`() {
            val codes = totpService.generateRecoveryCodes()
            assertThat(codes).doesNotHaveDuplicates()
        }

        @Test
        fun `should hash and verify recovery codes correctly`() {
            val codes = totpService.generateRecoveryCodes()
            codes.forEach { code: String ->
                val hash = totpService.hashRecoveryCode(code)
                assertThat(totpService.verifyRecoveryCode(code, hash)).isTrue
            }
        }

        @Test
        fun `should verify recovery code case-insensitively`() {
            val code = totpService.generateRecoveryCodes().first()
            val hash = totpService.hashRecoveryCode(code)

            assertThat(totpService.verifyRecoveryCode(code.lowercase(), hash)).isTrue
        }
    }
}
