package com.krachtix.identity.core.navigation.query

import an.awesome.pipelinr.Command

data class GetNavigationQuery(
    val platform: String = "WEB"
) : Command<GetNavigationResult>

data class GetNavigationResult(
    val items: List<NavigationItemDto>
)

data class NavigationItemDto(
    val key: String,
    val label: String,
    val icon: String?,
    val path: String?,
    val children: List<NavigationItemDto>
)
