package com.krachtix.identity.core.totp.service

import dev.samstevens.totp.code.CodeVerifier
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class TotpService(
    private val passwordEncoder: PasswordEncoder,
    @Value("\${identity.totp.issuer:Krachtix}") private val issuer: String,
    @Value("\${identity.totp.time-period:30}") private val timePeriod: Int = 30,
    @Value("\${identity.totp.allowed-discrepancy:1}") private val allowedDiscrepancy: Int = 1
) {
    private val secretGenerator = DefaultSecretGenerator(32)
    private val codeVerifier: CodeVerifier = DefaultCodeVerifier(
        DefaultCodeGenerator(HashingAlgorithm.SHA1, 6),
        SystemTimeProvider()
    ).apply {
        setAllowedTimePeriodDiscrepancy(allowedDiscrepancy)
    }
    private val secureRandom = SecureRandom()

    fun generateSecret(): String = secretGenerator.generate()

    fun generateQrCodeUri(secret: String, email: String): String =
        "otpauth://totp/${issuer}:${email}?secret=${secret}&issuer=${issuer}&algorithm=SHA1&digits=6&period=${timePeriod}"

    fun verifyCode(secret: String, code: String): Boolean = codeVerifier.isValidCode(secret, code)

    fun generateRecoveryCodes(count: Int = 8): List<String> =
        (1..count).map { generateRecoveryCode() }

    fun hashRecoveryCode(code: String): String = passwordEncoder.encode(normalizeRecoveryCode(code))!!

    fun verifyRecoveryCode(rawCode: String, hash: String): Boolean =
        passwordEncoder.matches(normalizeRecoveryCode(rawCode), hash)

    private fun generateRecoveryCode(): String {
        val part1 = generateRandomAlphanumeric(4)
        val part2 = generateRandomAlphanumeric(4)
        return "${part1}-${part2}".uppercase()
    }

    private fun generateRandomAlphanumeric(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
    }

    private fun normalizeRecoveryCode(code: String): String = code.replace("-", "").uppercase()
}
