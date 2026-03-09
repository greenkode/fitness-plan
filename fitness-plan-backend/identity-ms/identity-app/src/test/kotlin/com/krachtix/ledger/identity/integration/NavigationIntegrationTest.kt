package com.krachtix.identity.integration

import com.krachtix.identity.config.BaseIntegrationTest
import com.krachtix.identity.core.navigation.domain.NavigationItemRepository
import com.krachtix.identity.core.navigation.query.GetNavigationQuery
import com.krachtix.identity.core.navigation.query.GetNavigationQueryHandler
import com.krachtix.identity.core.service.UserService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var navigationItemRepository: NavigationItemRepository

    @MockitoBean
    lateinit var userService: UserService

    @Autowired
    lateinit var handler: GetNavigationQueryHandler

    @Nested
    @DisplayName("Navigation filtered by roles")
    inner class NavigationByRoles {

        @Test
        fun `super admin sees all navigation items including team`() {
            val roles = listOf("ROLE_MERCHANT_SUPER_ADMIN", "ROLE_MERCHANT_ADMIN", "ROLE_MERCHANT_FINANCE_ADMIN", "ROLE_MERCHANT_USER")
            `when`(userService.getCurrentUserRoles()).thenReturn(roles)

            val result = handler.handle(GetNavigationQuery())

            val allChildren = result.items.flatMap { it.children }
            assertTrue(allChildren.any { it.key == "team" })
            assertTrue(allChildren.any { it.key == "dashboard" })
            assertTrue(allChildren.any { it.key == "transaction-templates" })
            assertTrue(allChildren.any { it.key == "campaigns" })
        }

        @Test
        fun `admin sees all except team`() {
            val roles = listOf("ROLE_MERCHANT_ADMIN", "ROLE_MERCHANT_FINANCE_ADMIN", "ROLE_MERCHANT_USER")
            `when`(userService.getCurrentUserRoles()).thenReturn(roles)

            val result = handler.handle(GetNavigationQuery())

            val allChildren = result.items.flatMap { it.children }
            assertTrue(allChildren.none { it.key == "team" })
            assertTrue(allChildren.any { it.key == "transaction-templates" })
            assertTrue(allChildren.any { it.key == "campaigns" })
            assertTrue(allChildren.any { it.key == "limits" })
            assertTrue(allChildren.any { it.key == "profiles" })
        }

        @Test
        fun `finance admin does not see admin-only items`() {
            val roles = listOf("ROLE_MERCHANT_FINANCE_ADMIN", "ROLE_MERCHANT_USER")
            `when`(userService.getCurrentUserRoles()).thenReturn(roles)

            val result = handler.handle(GetNavigationQuery())

            val allChildren = result.items.flatMap { it.children }
            assertTrue(allChildren.none { it.key == "transaction-templates" })
            assertTrue(allChildren.none { it.key == "campaigns" })
            assertTrue(allChildren.none { it.key == "limits" })
            assertTrue(allChildren.none { it.key == "profiles" })
            assertTrue(allChildren.none { it.key == "team" })
            assertTrue(allChildren.any { it.key == "dashboard" })
            assertTrue(allChildren.any { it.key == "journal-entries" })
        }

        @Test
        fun `user sees same as finance admin`() {
            val roles = listOf("ROLE_MERCHANT_USER")
            `when`(userService.getCurrentUserRoles()).thenReturn(roles)

            val result = handler.handle(GetNavigationQuery())

            val allChildren = result.items.flatMap { it.children }
            assertTrue(allChildren.none { it.key == "transaction-templates" })
            assertTrue(allChildren.none { it.key == "campaigns" })
            assertTrue(allChildren.none { it.key == "limits" })
            assertTrue(allChildren.none { it.key == "profiles" })
            assertTrue(allChildren.none { it.key == "team" })
            assertTrue(allChildren.any { it.key == "dashboard" })
            assertTrue(allChildren.any { it.key == "customers" })
            assertTrue(allChildren.any { it.key == "accounts" })
        }

        @Test
        fun `navigation items are ordered correctly`() {
            val roles = listOf("ROLE_MERCHANT_SUPER_ADMIN", "ROLE_MERCHANT_ADMIN", "ROLE_MERCHANT_FINANCE_ADMIN", "ROLE_MERCHANT_USER")
            `when`(userService.getCurrentUserRoles()).thenReturn(roles)

            val result = handler.handle(GetNavigationQuery())

            assertEquals("main", result.items[0].key)
            assertEquals("accounting", result.items[1].key)
            assertEquals("reports", result.items[2].key)
            assertEquals("settings", result.items[3].key)
        }

        @Test
        fun `sections with no visible children are excluded`() {
            val roles = listOf("ROLE_MERCHANT_USER")
            `when`(userService.getCurrentUserRoles()).thenReturn(roles)

            val result = handler.handle(GetNavigationQuery())

            result.items.forEach { section ->
                assertTrue(section.children.isNotEmpty() || section.path != null,
                    "Section ${section.key} should have children or a path")
            }
        }
    }
}
