package com.krachtix.commons.cache

import java.time.Duration
import java.util.concurrent.TimeUnit

object CacheNames {
    const val ACCOUNT = "krachtix-core-accounts"
    const val PRICING_CALC = "krachtix-core-PricingCalc"
    const val USER_DETAILS = "krachtix-core-UserDetails"
    const val ACCOUNT_BALANCE = "krachtix-core-AccountBalance"
    const val POOL_ACCOUNT = "krachtix-core-PoolAccount"
    const val SYSTEM_PROPERTIES = "krachtix-core-SystemProps"
    const val AUTH_TOKEN = "krachtix-core-AuthToken"
    const val KYC_USER = "krachtix-identity-KycUser"
    const val MERCHANT_DETAILS = "krachtix-identity-MerchantDetails"
    const val PROCESS = "krachtix-core-Process"
    const val WEBHOOK_CONFIG = "krachtix-core-WebhookConfig"
    const val INTEGRATION_RESPONSE_CODE = "krachtix-core-IntegrationResponseCode"
    const val CHAT_SESSION = "krachtix-core-ChatSession"
    const val CHAT_MESSAGES = "krachtix-core-ChatMessages"
    const val CHAT_RESPONSES = "krachtix-core-ChatResponses"
    const val AGENT_CONFIG = "krachtix-core-AgentConfig"
    const val CHART_TREE = "krachtix-core-chart-tree"
    const val MINIGL_BALANCE_SNAPSHOTS = "krachtix-minigl-balance-snapshots"
    const val MINIGL_TRANSACTIONS = "krachtix-minigl-transactions"
    const val ACCOUNT_LIMITS = "krachtix-minigl-account-limits"
    const val MINIGL_ACCOUNT_PROFILES = "krachtix-minigl-account-profiles"
    const val MINIGL_TEMPLATES = "krachtix-minigl-templates"
    const val MINIGL_CURRENCIES = "krachtix-minigl-currencies"
    const val MERCHANT_TEMPLATES = "krachtix-core-MerchantTemplates"
    const val TEMPLATE_PRICING = "krachtix-core-template-pricing"

    const val USER_SETTINGS = "krachtix-identity-UserSettings"
    const val MERCHANT_CLIENT = "krachtix-identity-MerchantClient"
    const val OAUTH_USER = "krachtix-identity-OAuthUser"
    const val USER_ROLES = "krachtix-identity-UserRoles"
    const val REGISTERED_CLIENT = "krachtix-identity-RegisteredClient"
    const val COUNTRIES = "krachtix-identity-Countries"
    const val MERCHANT_SETTINGS = "krachtix-core-MerchantSettings"
    const val BILLING_PLAN = "krachtix-core-BillingPlan"
    const val MERCHANT_RESTRICTION = "krachtix-core-MerchantRestriction"
}

enum class SrCache(val cacheName: String, val ttl: Long, val timeUnit: TimeUnit) {

    ACCOUNT(com.krachtix.commons.cache.CacheNames.ACCOUNT, 60, TimeUnit.MINUTES),
    
    // Tier 1: Critical Path Performance (Highest Impact)
    PRICING_CALC(com.krachtix.commons.cache.CacheNames.PRICING_CALC, 10, TimeUnit.MINUTES),
    USER_DETAILS(com.krachtix.commons.cache.CacheNames.USER_DETAILS, 15, TimeUnit.MINUTES),
    PROCESS(com.krachtix.commons.cache.CacheNames.PROCESS, 5, TimeUnit.MINUTES),
    
    // Tier 2: High-Frequency Data  
    ACCOUNT_BALANCE(com.krachtix.commons.cache.CacheNames.ACCOUNT_BALANCE, 5, TimeUnit.MINUTES),
    POOL_ACCOUNT(com.krachtix.commons.cache.CacheNames.POOL_ACCOUNT, 2, TimeUnit.HOURS),
    
    // Tier 3: Configuration & Metadata
    SYSTEM_PROPERTIES(com.krachtix.commons.cache.CacheNames.SYSTEM_PROPERTIES, 10, TimeUnit.MINUTES),
    AUTH_TOKEN(com.krachtix.commons.cache.CacheNames.AUTH_TOKEN, 30, TimeUnit.MINUTES),
    KYC_USER(com.krachtix.commons.cache.CacheNames.KYC_USER, 20, TimeUnit.MINUTES),
    MERCHANT_DETAILS(com.krachtix.commons.cache.CacheNames.MERCHANT_DETAILS, 15, TimeUnit.MINUTES),
    WEBHOOK_CONFIG(com.krachtix.commons.cache.CacheNames.WEBHOOK_CONFIG, 30, TimeUnit.MINUTES),
    INTEGRATION_RESPONSE_CODE(com.krachtix.commons.cache.CacheNames.INTEGRATION_RESPONSE_CODE, 4, TimeUnit.HOURS),
    CHAT_SESSION(com.krachtix.commons.cache.CacheNames.CHAT_SESSION, 30, TimeUnit.MINUTES),
    CHAT_MESSAGES(com.krachtix.commons.cache.CacheNames.CHAT_MESSAGES, 30, TimeUnit.MINUTES),
    CHAT_RESPONSES(com.krachtix.commons.cache.CacheNames.CHAT_RESPONSES, 30, TimeUnit.MINUTES),
    AGENT_CONFIG(com.krachtix.commons.cache.CacheNames.AGENT_CONFIG, 24, TimeUnit.HOURS),

    // Chart tree cache
    CHART_TREE(com.krachtix.commons.cache.CacheNames.CHART_TREE, 2, TimeUnit.MINUTES),

    // MiniGL specific caches
    MINIGL_BALANCE_SNAPSHOTS(com.krachtix.commons.cache.CacheNames.MINIGL_BALANCE_SNAPSHOTS, 30, TimeUnit.MINUTES),
    MINIGL_TRANSACTIONS(com.krachtix.commons.cache.CacheNames.MINIGL_TRANSACTIONS, 30, TimeUnit.MINUTES),
    ACCOUNT_LIMITS(com.krachtix.commons.cache.CacheNames.ACCOUNT_LIMITS, 30, TimeUnit.MINUTES),
    MINIGL_ACCOUNT_PROFILES(com.krachtix.commons.cache.CacheNames.MINIGL_ACCOUNT_PROFILES, 120, TimeUnit.MINUTES),
    MINIGL_TEMPLATES(com.krachtix.commons.cache.CacheNames.MINIGL_TEMPLATES, 120, TimeUnit.MINUTES),
    MINIGL_CURRENCIES(com.krachtix.commons.cache.CacheNames.MINIGL_CURRENCIES, 240, TimeUnit.MINUTES),
    MERCHANT_TEMPLATES(com.krachtix.commons.cache.CacheNames.MERCHANT_TEMPLATES, 30, TimeUnit.MINUTES),
    TEMPLATE_PRICING(com.krachtix.commons.cache.CacheNames.TEMPLATE_PRICING, 30, TimeUnit.MINUTES),

    // Identity specific caches
    USER_SETTINGS(com.krachtix.commons.cache.CacheNames.USER_SETTINGS, 15, TimeUnit.MINUTES),
    MERCHANT_CLIENT(com.krachtix.commons.cache.CacheNames.MERCHANT_CLIENT, 30, TimeUnit.MINUTES),
    OAUTH_USER(com.krachtix.commons.cache.CacheNames.OAUTH_USER, 10, TimeUnit.MINUTES),
    USER_ROLES(com.krachtix.commons.cache.CacheNames.USER_ROLES, 20, TimeUnit.MINUTES),
    REGISTERED_CLIENT(com.krachtix.commons.cache.CacheNames.REGISTERED_CLIENT, 30, TimeUnit.MINUTES),
    COUNTRIES(com.krachtix.commons.cache.CacheNames.COUNTRIES, 24, TimeUnit.HOURS),

    MERCHANT_SETTINGS(com.krachtix.commons.cache.CacheNames.MERCHANT_SETTINGS, 15, TimeUnit.MINUTES),

    BILLING_PLAN(com.krachtix.commons.cache.CacheNames.BILLING_PLAN, 15, TimeUnit.MINUTES),

    MERCHANT_RESTRICTION(com.krachtix.commons.cache.CacheNames.MERCHANT_RESTRICTION, 2, TimeUnit.MINUTES);

    fun computeTtl(ttlTimeUnit: TimeUnit, ttl: Long): Duration {
        return when (ttlTimeUnit) {
            TimeUnit.SECONDS -> Duration.ofSeconds(ttl)
            TimeUnit.MINUTES -> Duration.ofMinutes(ttl)
            TimeUnit.HOURS -> Duration.ofHours(ttl)
            TimeUnit.DAYS -> Duration.ofDays(ttl)
            else -> Duration.ofSeconds(60)
        }
    }
}