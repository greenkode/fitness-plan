package com.krachtix.identity.core.navigation.api

import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.core.navigation.dto.NavigationItemResponse
import com.krachtix.identity.core.navigation.dto.NavigationResponse
import com.krachtix.identity.core.navigation.query.GetNavigationQuery
import com.krachtix.identity.core.navigation.query.NavigationItemDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/navigation")
class NavigationController(
    private val pipeline: Pipeline
) {

    @GetMapping
    fun getNavigation(
        @RequestParam(defaultValue = "WEB") platform: String
    ): NavigationResponse {
        log.debug { "Fetching navigation for platform=$platform" }

        val result = pipeline.send(GetNavigationQuery(platform = platform))

        return NavigationResponse(
            items = result.items.map { it.toResponse() }
        )
    }

    private fun NavigationItemDto.toResponse(): NavigationItemResponse = NavigationItemResponse(
        key = key,
        label = label,
        icon = icon,
        path = path,
        children = children.map { it.toResponse() }
    )
}
