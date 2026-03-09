package com.krachtix.identity.core.totp.domain

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Converter
class TotpSecretConverter : AttributeConverter<String?, String?> {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12

        @Volatile
        private var encryptionKey: String = "change-me-in-production-32chars!"

        fun setEncryptionKey(key: String) {
            encryptionKey = key
        }

        private fun getKeySpec(): SecretKeySpec {
            val keyBytes = encryptionKey.toByteArray(Charsets.UTF_8).copyOf(32)
            return SecretKeySpec(keyBytes, "AES")
        }
    }

    override fun convertToDatabaseColumn(attribute: String?): String? {
        return attribute?.let { encrypt(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return dbData?.let { decrypt(it) }
    }

    private fun encrypt(plaintext: String): String {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, getKeySpec(), GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val buffer = ByteBuffer.allocate(iv.size + ciphertext.size)
        buffer.put(iv)
        buffer.put(ciphertext)

        return Base64.getEncoder().encodeToString(buffer.array())
    }

    private fun decrypt(encrypted: String): String {
        val decoded = Base64.getDecoder().decode(encrypted)
        val buffer = ByteBuffer.wrap(decoded)

        val iv = ByteArray(GCM_IV_LENGTH)
        buffer.get(iv)

        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, getKeySpec(), GCMParameterSpec(GCM_TAG_LENGTH, iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
