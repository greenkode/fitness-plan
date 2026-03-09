package com.krachtix.identity.core.invitation.query

import com.krachtix.identity.commons.RoleEnum
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.identity.core.invitation.dto.UserListItem
import com.krachtix.identity.core.invitation.dto.UserStatus
import com.krachtix.identity.core.repository.OAuthUserRepository
import com.krachtix.identity.core.service.UserService
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
class GetUsersQueryHandler(
    private val userRepository: OAuthUserRepository,
    private val userService: UserService
) : Command.Handler<GetUsersQuery, GetUsersResult> {

    override fun handle(query: GetUsersQuery): GetUsersResult {
        log.info { "Processing GetUsersQuery" }

        val currentUser = userService.getCurrentUser()
        val merchantId = currentUser.merchantId
            ?: throw RecordNotFoundException("User is not associated with a merchant")

        val users = userRepository.findByMerchantId(merchantId)
            .filter { it.enabled }
            .map { user ->
                val userRoles =
                    user.authorities.filter { it.startsWith("ROLE_") && RoleEnum.of(it) != null }
                        .joinToString(", ") { RoleEnum.of(it)?.description.toString() }

                val status = if (user.invitationStatus) UserStatus.ACTIVE else UserStatus.PENDING

                UserListItem(
                    id = user.id.toString(),
                    username = user.username,
                    fullName = user.fullName(),
                    role = userRoles.ifEmpty { "No roles assigned" },
                    status = status,
                    createdAt = user.createdAt
                )
            }

        return GetUsersResult(users)
    }
}