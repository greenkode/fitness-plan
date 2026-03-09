package com.krachtix.identity.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.entity.WebhookConfigEntity
import com.krachtix.identity.core.entity.WebhookConfigStatus
import com.krachtix.identity.core.repository.WebhookConfigRepository
import com.krachtix.identity.core.webhook.dto.ActiveWebhookDto
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger {}

@Service
@Transactional
class WebhookConfigService(
    private val webhookConfigRepository: WebhookConfigRepository,
    private val messageService: MessageService,
    @Value("\${app.webhook.secret-key:default-webhook-encryption-key!!}") private val encryptionKey: String
) {

    companion object {
        private const val MAX_WEBHOOKS_PER_CLIENT = 20
        private const val SECRET_LENGTH = 32
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    fun createWebhook(
        clientId: UUID,
        name: String,
        url: String,
        eventTypes: List<String>,
        description: String?
    ): Pair<WebhookConfigEntity, CharArray> {
        log.info { "Creating webhook config for client: $clientId" }

        val existingCount = webhookConfigRepository.countByClientIdAndStatusNot(clientId, WebhookConfigStatus.DELETED)
        if (existingCount >= MAX_WEBHOOKS_PER_CLIENT) {
            throw InvalidRequestException(messageService.getMessage("webhook.error.max_webhooks_reached"))
        }

        val secret = RandomStringUtils.secure().nextAlphanumeric(SECRET_LENGTH).toCharArray()
        val encryptedSecret = encrypt(String(secret))

        val entity = WebhookConfigEntity().apply {
            this.clientId = clientId
            this.name = name
            this.url = url
            this.secretHash = encryptedSecret
            this.status = WebhookConfigStatus.ACTIVE
            this.description = description
            this.setEventTypeList(eventTypes)
        }

        val saved = webhookConfigRepository.save(entity)
        log.info { "Webhook config created with publicId: ${saved.publicId}" }

        return saved to secret
    }

    fun updateWebhook(
        publicId: UUID,
        clientId: UUID,
        name: String?,
        url: String?,
        eventTypes: List<String>?,
        description: String?
    ): WebhookConfigEntity {
        log.info { "Updating webhook config: $publicId" }

        val entity = findWebhookForClient(publicId, clientId)

        when (entity.status) {
            WebhookConfigStatus.DELETED -> throw InvalidRequestException(
                messageService.getMessage("webhook.error.already_deleted")
            )
            else -> Unit
        }

        name?.let { entity.name = it }
        url?.let { entity.url = it }
        eventTypes?.let { entity.setEventTypeList(it) }
        description?.let { entity.description = it }

        return webhookConfigRepository.save(entity)
    }

    fun deleteWebhook(publicId: UUID, clientId: UUID): WebhookConfigEntity {
        log.info { "Soft-deleting webhook config: $publicId" }

        val entity = findWebhookForClient(publicId, clientId)

        when (entity.status) {
            WebhookConfigStatus.DELETED -> throw InvalidRequestException(
                messageService.getMessage("webhook.error.already_deleted")
            )
            else -> Unit
        }

        entity.status = WebhookConfigStatus.DELETED
        return webhookConfigRepository.save(entity)
    }

    fun rotateSecret(publicId: UUID, clientId: UUID): Pair<WebhookConfigEntity, CharArray> {
        log.info { "Rotating secret for webhook config: $publicId" }

        val entity = findWebhookForClient(publicId, clientId)

        when (entity.status) {
            WebhookConfigStatus.DELETED -> throw InvalidRequestException(
                messageService.getMessage("webhook.error.already_deleted")
            )
            else -> Unit
        }

        val newSecret = RandomStringUtils.secure().nextAlphanumeric(SECRET_LENGTH).toCharArray()
        entity.secretHash = encrypt(String(newSecret))

        val saved = webhookConfigRepository.save(entity)
        return saved to newSecret
    }

    @Transactional(readOnly = true)
    fun getWebhook(publicId: UUID, clientId: UUID): WebhookConfigEntity =
        findWebhookForClient(publicId, clientId)

    @Transactional(readOnly = true)
    fun listWebhooks(clientId: UUID, page: Int, size: Int): Page<WebhookConfigEntity> {
        log.info { "Listing webhooks for client: $clientId, page: $page, size: $size" }
        return webhookConfigRepository.findByClientId(clientId, PageRequest.of(page, size))
    }

    @Transactional(readOnly = true)
    fun getActiveWebhooksForEvent(clientId: UUID, eventType: String): List<WebhookConfigEntity> {
        log.info { "Fetching active webhooks for client: $clientId, eventType: $eventType" }
        return webhookConfigRepository.findByClientIdAndStatusAndEventTypesContaining(
            clientId,
            WebhookConfigStatus.ACTIVE,
            eventType
        )
    }

    @Transactional(readOnly = true)
    fun getActiveWebhookDtosForEvent(clientId: UUID, eventType: String): List<ActiveWebhookDto> =
        getActiveWebhooksForEvent(clientId, eventType).map { entity ->
            ActiveWebhookDto(
                publicId = entity.publicId,
                url = entity.url,
                signingSecret = decryptSecret(entity.secretHash),
                eventTypes = entity.getEventTypeList()
            )
        }

    @Transactional(readOnly = true)
    fun getWebhookDtoByPublicId(publicId: UUID): ActiveWebhookDto? =
        webhookConfigRepository.findByPublicId(publicId)?.let { entity ->
            ActiveWebhookDto(
                publicId = entity.publicId,
                url = entity.url,
                signingSecret = decryptSecret(entity.secretHash),
                eventTypes = entity.getEventTypeList()
            )
        }

    fun decryptSecret(encryptedSecret: String): String = decrypt(encryptedSecret)

    private fun findWebhookForClient(publicId: UUID, clientId: UUID): WebhookConfigEntity {
        val entity = webhookConfigRepository.findByPublicId(publicId)
            ?: throw RecordNotFoundException(messageService.getMessage("webhook.error.not_found"))

        if (entity.clientId != clientId) {
            throw RecordNotFoundException(messageService.getMessage("webhook.error.not_found"))
        }

        return entity
    }

    private fun encrypt(plaintext: String): String {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val keySpec = buildKeySpec()
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))

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

        val keySpec = buildKeySpec()
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun buildKeySpec(): SecretKeySpec {
        val keyBytes = encryptionKey.toByteArray(Charsets.UTF_8).copyOf(32)
        return SecretKeySpec(keyBytes, "AES")
    }
}
