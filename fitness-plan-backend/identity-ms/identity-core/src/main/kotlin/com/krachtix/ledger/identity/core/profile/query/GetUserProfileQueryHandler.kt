package com.krachtix.identity.core.profile.query

import com.krachtix.commons.fileupload.FileUploadGateway
import com.krachtix.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional(readOnly = true)
class GetUserProfileQueryHandler(
    private val userService: UserService,
    private val fileUploadGateway: FileUploadGateway
) : Command.Handler<GetUserProfileQuery, GetUserProfileResult> {

    companion object {
        private const val PRESIGNED_URL_EXPIRATION_MINUTES = 60L
    }

    override fun handle(command: GetUserProfileQuery): GetUserProfileResult {
        log.info { "Getting user profile" }

        val user = userService.getCurrentUser()

        val pictureUrl = user.pictureUrl?.let { resolveAvatarUrl(it) }

        return GetUserProfileResult(
            id = user.id.toString(),
            username = user.username,
            email = user.email?.value ?: "",
            firstName = user.firstName,
            lastName = user.lastName,
            phoneNumber = user.phoneNumber?.e164,
            pictureUrl = pictureUrl,
            locale = user.locale,
            emailVerified = user.emailVerified,
            registrationSource = user.registrationSource.name
        )
    }

    private fun resolveAvatarUrl(storedValue: String): String {
        return when {
            storedValue.startsWith("http") -> storedValue
            storedValue.startsWith("avatars/") -> fileUploadGateway.generatePresignedDownloadUrl(
                storedValue,
                PRESIGNED_URL_EXPIRATION_MINUTES
            )
            else -> storedValue
        }
    }
}
