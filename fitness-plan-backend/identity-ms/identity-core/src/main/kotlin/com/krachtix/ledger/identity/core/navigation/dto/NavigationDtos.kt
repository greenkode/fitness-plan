package com.krachtix.identity.core.navigation.dto

data class NavigationResponse(
    val items: List<NavigationItemResponse>
)

data class NavigationItemResponse(
    val key: String,
    val label: String,
    val icon: String?,
    val path: String?,
    val children: List<NavigationItemResponse>
)
