package com.krachtix.identity.core.navigation.query

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.core.navigation.domain.NavigationItemEntity
import com.krachtix.identity.core.navigation.domain.NavigationItemRepository
import com.krachtix.identity.core.service.UserService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetNavigationQueryHandler(
    private val navigationItemRepository: NavigationItemRepository,
    private val userService: UserService
) : Command.Handler<GetNavigationQuery, GetNavigationResult> {

    override fun handle(query: GetNavigationQuery): GetNavigationResult {
        val roles = userService.getCurrentUserRoles().toSet()

        log.debug { "Fetching navigation for platform=${query.platform}, roles=$roles" }

        val items = navigationItemRepository.findAccessibleItems(query.platform, roles)

        val tree = buildNavigationTree(items)

        return GetNavigationResult(items = tree)
    }

    private fun buildNavigationTree(items: List<NavigationItemEntity>): List<NavigationItemDto> {
        val parentItems = items.filter { it.parent == null }
        val childrenByParentId = items
            .filter { it.parent != null }
            .groupBy { it.parent!!.id }

        return parentItems.mapNotNull { parent ->
            val children = childrenByParentId[parent.id]
                ?.sortedBy { it.sortOrder }
                ?.map { it.toDto() }
                ?: emptyList()

            when {
                parent.path != null -> parent.toDto()
                children.isNotEmpty() -> NavigationItemDto(
                    key = parent.key,
                    label = parent.label,
                    icon = parent.icon,
                    path = null,
                    children = children
                )
                else -> null
            }
        }
    }

    private fun NavigationItemEntity.toDto() = NavigationItemDto(
        key = key,
        label = label,
        icon = icon,
        path = path,
        children = emptyList()
    )
}
