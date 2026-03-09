package com.krachtix.identity.core.config

enum class JwtClaimName(val value: String) {
    MERCHANT_ID("merchant_id"),
    ORGANIZATION_ID("organization_id"),
    ENVIRONMENT("environment"),
    TYPE("type"),
    CLIENT_TYPE("client_type"),
    ENVIRONMENT_CONFIG("environment_config"),
    ENVIRONMENT_PREFERENCE("environment_preference"),
    MERCHANT_ENVIRONMENT_MODE("merchant_environment_mode"),
    REALM_ACCESS("realm_access"),
    RESOURCE_ACCESS("resource_access"),
    AUTHORITIES("authorities"),
    ROLES("roles"),
    SETUP_COMPLETED("setup_completed"),
    ORGANIZATION_STATUS("organization_status"),
    TOTP_ENABLED("totp_enabled"),
    TWO_FACTOR_LAST_VERIFIED("two_factor_last_verified"),
    BILLING_TIER("billing_tier")
}
