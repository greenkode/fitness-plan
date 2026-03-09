package com.krachtix.identity.core.totp.domain

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TotpEncryptionConfig(
    @Value("\${identity.totp.encryption-key:change-me-in-production-32chars!}") private val encryptionKey: String
) {

    @PostConstruct
    fun init() {
        TotpSecretConverter.setEncryptionKey(encryptionKey)
    }
}
