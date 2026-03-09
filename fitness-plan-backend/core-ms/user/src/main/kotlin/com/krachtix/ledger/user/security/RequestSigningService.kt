package com.krachtix.user.security

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger {}

@Service
class RequestSigningService(
    @Value("\${security.request-signing.algorithm:HmacSHA256}")
    private val algorithm: String,
    @Value("\${security.request-signing.timestamp-tolerance-seconds:300}")
    private val timestampToleranceSeconds: Long
) {

    companion object {
        const val SIGNATURE_HEADER = "X-Signature"
        const val TIMESTAMP_HEADER = "X-Timestamp"
        const val NONCE_HEADER = "X-Nonce"
    }

    fun generateSignature(
        secretKey: String,
        method: String,
        path: String,
        body: String?,
        timestamp: Long,
        nonce: String
    ): String {
        val payload = buildPayload(method, path, body, timestamp, nonce)
        return computeHmac(secretKey, payload)
    }

    fun validateSignature(
        secretKey: String,
        method: String,
        path: String,
        body: String?,
        timestamp: Long,
        nonce: String,
        providedSignature: String
    ): SignatureValidationResult {
        if (!isTimestampValid(timestamp)) {
            log.warn { "Request signature timestamp is outside acceptable range: $timestamp" }
            return SignatureValidationResult.TIMESTAMP_EXPIRED
        }

        val expectedSignature = generateSignature(secretKey, method, path, body, timestamp, nonce)

        return if (constantTimeEquals(expectedSignature, providedSignature)) {
            SignatureValidationResult.VALID
        } else {
            log.warn { "Request signature mismatch for $method $path" }
            SignatureValidationResult.INVALID_SIGNATURE
        }
    }

    private fun buildPayload(method: String, path: String, body: String?, timestamp: Long, nonce: String): String {
        return buildString {
            append(method.uppercase())
            append("\n")
            append(path)
            append("\n")
            append(timestamp)
            append("\n")
            append(nonce)
            if (!body.isNullOrBlank()) {
                append("\n")
                append(body)
            }
        }
    }

    private fun computeHmac(secretKey: String, payload: String): String {
        val mac = Mac.getInstance(algorithm)
        val keySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), algorithm)
        mac.init(keySpec)
        val hash = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun isTimestampValid(timestamp: Long): Boolean {
        val now = Instant.now().epochSecond
        return kotlin.math.abs(now - timestamp) <= timestampToleranceSeconds
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    enum class SignatureValidationResult {
        VALID,
        INVALID_SIGNATURE,
        TIMESTAMP_EXPIRED,
        MISSING_HEADERS
    }
}
