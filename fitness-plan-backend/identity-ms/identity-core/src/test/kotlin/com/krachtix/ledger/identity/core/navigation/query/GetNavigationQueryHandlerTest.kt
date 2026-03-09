package com.krachtix.identity.core.navigation.query

import com.krachtix.identity.core.navigation.domain.NavigationItemEntity
import com.krachtix.identity.core.navigation.domain.NavigationItemRepository
import com.krachtix.identity.core.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class GetNavigationQueryHandlerTest {

    @Mock
    lateinit var navigationItemRepository: NavigationItemRepository

    @Mock
    lateinit var userService: UserService

    private lateinit var handler: GetNavigationQueryHandler

    @BeforeEach
    fun setUp() {
        handler = GetNavigationQueryHandler(navigationItemRepository, userService)
    }

    private fun buildSection(id: Int, key: String, label: String, sortOrder: Int) = NavigationItemEntity(
        id = id,
        key = key,
        label = label,
        sortOrder = sortOrder,
        active = true,
        platform = "WEB"
    )

    private fun buildItem(id: Int, key: String, label: String, icon: String, path: String, parent: NavigationItemEntity, sortOrder: Int, roles: Set<String>) = NavigationItemEntity(
        id = id,
        key = key,
        label = label,
        icon = icon,
        path = path,
        parent = parent,
        sortOrder = sortOrder,
        active = true,
        platform = "WEB",
        roles = roles
    )

    private fun allNavigationItems(): List<NavigationItemEntity> {
        val allRoles = setOf("ROLE_MERCHANT_USER", "ROLE_MERCHANT_FINANCE_ADMIN", "ROLE_MERCHANT_ADMIN", "ROLE_MERCHANT_SUPER_ADMIN")
        val adminRoles = setOf("ROLE_MERCHANT_ADMIN", "ROLE_MERCHANT_SUPER_ADMIN")
        val superAdminRoles = setOf("ROLE_MERCHANT_SUPER_ADMIN")

        val main = buildSection(1, "main", "Main", 0)
        val accounting = buildSection(2, "accounting", "Accounting", 1)
        val reports = buildSection(3, "reports", "Reports", 2)
        val settings = buildSection(4, "settings", "Settings", 3)

        return listOf(
            main, accounting, reports, settings,
            buildItem(5, "dashboard", "Dashboard", "mdi-view-dashboard", "/dashboard", main, 0, allRoles),
            buildItem(6, "customers", "Customers", "mdi-account-group", "/customers", main, 1, allRoles),
            buildItem(7, "accounts", "Accounts", "mdi-wallet", "/accounts", main, 2, allRoles),
            buildItem(8, "transaction-history", "Transactions", "mdi-swap-horizontal", "/transaction-history", main, 3, allRoles),
            buildItem(9, "transaction-templates", "Transaction Templates", "mdi-swap-horizontal", "/transactions", accounting, 0, adminRoles),
            buildItem(10, "campaigns", "Campaigns", "mdi-bullhorn-variant", "/campaigns", accounting, 1, adminRoles),
            buildItem(11, "limits", "Limits", "mdi-shield-check", "/limits", accounting, 2, adminRoles),
            buildItem(12, "profiles", "Profiles", "mdi-account-group", "/profiles", accounting, 3, adminRoles),
            buildItem(13, "journal-entries", "Journal Entries", "mdi-notebook-outline", "/journal", accounting, 4, allRoles),
            buildItem(14, "chart-of-accounts", "Chart of Accounts", "mdi-book-open-page-variant", "/reports/chart-of-accounts", reports, 0, allRoles),
            buildItem(15, "trial-balance", "Trial Balance", "mdi-table-check", "/reports/trial-balance", reports, 1, allRoles),
            buildItem(16, "balance-sheet", "Balance Sheet", "mdi-scale-balance", "/reports/balance-sheet", reports, 2, allRoles),
            buildItem(17, "income-statement", "Income Statement", "mdi-chart-line", "/reports/income-statement", reports, 3, allRoles),
            buildItem(18, "organization", "Organization", "mdi-cog", "/settings/organization", settings, 0, allRoles),
            buildItem(19, "profile", "Profile", "mdi-cog", "/settings/profile", settings, 1, allRoles),
            buildItem(20, "team", "Team", "mdi-cog", "/settings/team", settings, 2, superAdminRoles)
        )
    }

    @Nested
    @DisplayName("Super Admin Navigation")
    inner class SuperAdminNavigation {

        @Test
        fun `super admin sees all navigation items`() {
            val roles = listOf("ROLE_MERCHANT_SUPER_ADMIN", "ROLE_MERCHANT_ADMIN", "ROLE_MERCHANT_FINANCE_ADMIN", "ROLE_MERCHANT_USER")
            `when`(userService.getCurrentUserRoles()).thenReturn(roles)
            `when`(navigationItemRepository.findAccessibleItems("WEB", roles.toSet())).thenReturn(allNavigationItems())

            val result = handler.handle(GetNavigationQuery())

            assertEquals(4, result.items.size)
            val allChildren = result.items.flatMap { it.children }
            assertEquals(17, allChildren.size)
            assertTrue(allChildren.any { it.key == "team" })
        }
    }

    @Nested
    @DisplayName("Admin Navigation")
    inner class AdminNavigation {

        @Test
        fun `admin sees all except team`() {
            val roles = listOf("ROLE_MERCHANT_ADMIN", "ROLE_MERCHANT_FINANCE_ADMIN", "ROLE_MERCHANT_USER")
            `when`(userService.getCurrentUserRoles()).thenReturn(roles)

            val itemsWithoutTeam = allNavigationItems().filter { it.key != "team" }
            `when`(navigationItemRepository.findAccessibleItems("WEB", roles.toSet())).thenReturn(itemsWithoutTeam)

            val result = handler.handle(GetNavigationQuery())

            val allChildren = result.items.flatMap { it.children }
            assertEquals(16, allChildren.size)
            assertTrue(allChildren.none { it.key == "team" })
        }
    }

    @Nested
    @DisplayName("User Navigation")
    inner class UserNavigation {

        @Test
        fun `user does not see admin-only sections`() {
            val roles = listOf("ROLE_MERCHANT_USER")
            `when`(userService.getCurrentUserRoles()).thenReturn(roles)

            val userItems = allNavigationItems().filter { entity ->
                entity.path == null || entity.roles.contains("ROLE_MERCHANT_USER")
            }
            `when`(navigationItemRepository.findAccessibleItems("WEB", roles.toSet())).thenReturn(userItems)

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
    }

    @Nested
    @DisplayName("Empty Sections")
    inner class EmptySections {

        @Test
        fun `empty sections are filtered out`() {
            val roles = listOf("ROLE_MERCHANT_USER")
            `when`(userService.getCurrentUserRoles()).thenReturn(roles)

            val accounting = buildSection(2, "accounting", "Accounting", 1)
            `when`(navigationItemRepository.findAccessibleItems("WEB", roles.toSet())).thenReturn(listOf(accounting))

            val result = handler.handle(GetNavigationQuery())

            assertTrue(result.items.isEmpty())
        }
    }

    @Nested
    @DisplayName("Sort Order")
    inner class SortOrder {

        @Test
        fun `items are ordered by sort order`() {
            val roles = listOf("ROLE_MERCHANT_SUPER_ADMIN")
            `when`(userService.getCurrentUserRoles()).thenReturn(roles)

            val main = buildSection(1, "main", "Main", 0)
            val items = listOf(
                main,
                buildItem(5, "dashboard", "Dashboard", "mdi-view-dashboard", "/dashboard", main, 0, setOf("ROLE_MERCHANT_SUPER_ADMIN")),
                buildItem(6, "customers", "Customers", "mdi-account-group", "/customers", main, 1, setOf("ROLE_MERCHANT_SUPER_ADMIN"))
            )
            `when`(navigationItemRepository.findAccessibleItems("WEB", roles.toSet())).thenReturn(items)

            val result = handler.handle(GetNavigationQuery())

            assertEquals(1, result.items.size)
            assertEquals("dashboard", result.items[0].children[0].key)
            assertEquals("customers", result.items[0].children[1].key)
        }
    }
}
