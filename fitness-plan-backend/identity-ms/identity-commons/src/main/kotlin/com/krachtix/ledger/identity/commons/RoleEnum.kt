package com.krachtix.identity.commons

enum class RoleEnum(val description: String, val value: String) {

    ROLE_MERCHANT_USER("Viewer Role", "ROLE_MERCHANT_USER"),
    ROLE_MERCHANT_FINANCE_ADMIN("Finance Admin Role", "ROLE_MERCHANT_FINANCE_ADMIN"),
    ROLE_MERCHANT_ADMIN("Admin Role", "ROLE_MERCHANT_ADMIN"),
    ROLE_MERCHANT_SUPER_ADMIN("Super Admin Role", "ROLE_MERCHANT_SUPER_ADMIN"),
    ROLE_TWO_FACTOR_AUTH("Two Factor Auth Role", "ROLE_TWO_FACTOR_AUTH");

    companion object {
        fun of(value: String): RoleEnum? {
            return entries.firstOrNull { it.value == value }
        }
    }
}