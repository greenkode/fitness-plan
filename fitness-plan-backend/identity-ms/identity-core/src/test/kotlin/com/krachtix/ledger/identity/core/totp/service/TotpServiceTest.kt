package com.krachtix.identity.core.totp.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class TotpServiceTest {

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var totpService: TotpService

    @BeforeEach
    fun setUp() {
        totpService = TotpService(
            passwordEncoder = passwordEncoder,
            issuer = "Krachtix",
            timePeriod = 30,
            allowedDiscrepancy = 1
        )
    }

    @Nested
    inner class GenerateSecret {

        @Test
        fun `should return a non-empty string`() {
            val secret = totpService.generateSecret()

            assertThat(secret).isNotNull()
            assertThat(secret).isNotEmpty()
        }

        @Test
        fun `should return a valid Base32 encoded string`() {
            val secret = totpService.generateSecret()

            assertThat(secret).matches("[A-Z2-7]+=*")
        }

        @Test
        fun `should generate unique secrets on each call`() {
            val secret1 = totpService.generateSecret()
            val secret2 = totpService.generateSecret()

            assertThat(secret1).isNotEqualTo(secret2)
        }
    }

    @Nested
    inner class GenerateQrCodeUri {

        @Test
        fun `should return a valid otpauth URI`() {
            val secret = "JBSWY3DPEHPK3PXP"
            val email = "user@example.com"

            val uri = totpService.generateQrCodeUri(secret, email)

            assertThat(uri).startsWith("otpauth://totp/")
        }

        @Test
        fun `should contain the issuer in the URI`() {
            val secret = "JBSWY3DPEHPK3PXP"
            val email = "user@example.com"

            val uri = totpService.generateQrCodeUri(secret, email)

            assertThat(uri).contains("issuer=Krachtix")
            assertThat(uri).contains("Krachtix:user@example.com")
        }

        @Test
        fun `should contain the email in the URI`() {
            val secret = "JBSWY3DPEHPK3PXP"
            val email = "user@example.com"

            val uri = totpService.generateQrCodeUri(secret, email)

            assertThat(uri).contains(email)
        }

        @Test
        fun `should contain the secret in the URI`() {
            val secret = "JBSWY3DPEHPK3PXP"
            val email = "user@example.com"

            val uri = totpService.generateQrCodeUri(secret, email)

            assertThat(uri).contains("secret=$secret")
        }

        @Test
        fun `should contain algorithm and digits and period parameters`() {
            val secret = "JBSWY3DPEHPK3PXP"
            val email = "user@example.com"

            val uri = totpService.generateQrCodeUri(secret, email)

            assertThat(uri).contains("algorithm=SHA1")
            assertThat(uri).contains("digits=6")
            assertThat(uri).contains("period=30")
        }
    }

    @Nested
    inner class VerifyCode {

        @Test
        fun `should return false for an invalid code`() {
            val secret = totpService.generateSecret()

            val result = totpService.verifyCode(secret, "000000")

            assertThat(result).isFalse()
        }

        @Test
        fun `should return false for a non-numeric code`() {
            val secret = totpService.generateSecret()

            val result = totpService.verifyCode(secret, "abcdef")

            assertThat(result).isFalse()
        }

        @Test
        fun `should return false for an empty code`() {
            val secret = totpService.generateSecret()

            val result = totpService.verifyCode(secret, "")

            assertThat(result).isFalse()
        }
    }

    @Nested
    inner class GenerateRecoveryCodes {

        @Test
        fun `should return the default count of 8 recovery codes`() {
            val codes = totpService.generateRecoveryCodes()

            assertThat(codes).hasSize(8)
        }

        @Test
        fun `should return the specified count of recovery codes`() {
            val codes = totpService.generateRecoveryCodes(count = 5)

            assertThat(codes).hasSize(5)
        }

        @Test
        fun `should return codes in XXXX-XXXX format`() {
            val codes = totpService.generateRecoveryCodes()

            codes.forEach { code ->
                assertThat(code).matches("[A-Z0-9]{4}-[A-Z0-9]{4}")
            }
        }

        @Test
        fun `should return all unique codes`() {
            val codes = totpService.generateRecoveryCodes()

            assertThat(codes).doesNotHaveDuplicates()
        }

        @Test
        fun `should return uppercase codes`() {
            val codes = totpService.generateRecoveryCodes()

            codes.forEach { code ->
                assertThat(code).isEqualTo(code.uppercase())
            }
        }
    }

    @Nested
    inner class HashRecoveryCode {

        @Test
        fun `should return a non-null hash`() {
            val code = "ABCD-1234"
            `when`(passwordEncoder.encode("ABCD1234")).thenReturn("\$2a\$10\$hashedvalue")

            val hash = totpService.hashRecoveryCode(code)

            assertThat(hash).isNotNull()
        }

        @Test
        fun `should delegate to password encoder with normalized code`() {
            val code = "abcd-1234"
            `when`(passwordEncoder.encode("ABCD1234")).thenReturn("\$2a\$10\$hashedvalue")

            val hash = totpService.hashRecoveryCode(code)

            assertThat(hash).isEqualTo("\$2a\$10\$hashedvalue")
        }
    }

    @Nested
    inner class VerifyRecoveryCode {

        @Test
        fun `should return true when raw code matches hash`() {
            val rawCode = "ABCD-1234"
            val hash = "\$2a\$10\$hashedvalue"
            `when`(passwordEncoder.matches("ABCD1234", hash)).thenReturn(true)

            val result = totpService.verifyRecoveryCode(rawCode, hash)

            assertThat(result).isTrue()
        }

        @Test
        fun `should return false when raw code does not match hash`() {
            val rawCode = "WXYZ-9876"
            val hash = "\$2a\$10\$hashedvalue"
            `when`(passwordEncoder.matches("WXYZ9876", hash)).thenReturn(false)

            val result = totpService.verifyRecoveryCode(rawCode, hash)

            assertThat(result).isFalse()
        }

        @Test
        fun `should normalize the code before verification`() {
            val rawCode = "abcd-1234"
            val hash = "\$2a\$10\$hashedvalue"
            `when`(passwordEncoder.matches("ABCD1234", hash)).thenReturn(true)

            val result = totpService.verifyRecoveryCode(rawCode, hash)

            assertThat(result).isTrue()
        }
    }
}
