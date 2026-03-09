package com.krachtix.commons.webhook

enum class WebhookPayloadKey(val key: String) {
    REFERENCE("reference"),
    DISPLAY_REF("display_ref"),
    TYPE("type"),
    GROUP("group"),
    STATUS("status"),
    SENDER_AMOUNT("sender_amount"),
    RECIPIENT_AMOUNT("recipient_amount"),
    CURRENCY("currency"),
    AMOUNT("amount"),
    SENDER_ACCOUNT_PUBLIC_ID("sender_account_public_id"),
    RECIPIENT_ACCOUNT_PUBLIC_ID("recipient_account_public_id"),
    PUBLIC_ID("public_id"),
    CODE("code"),
    NAME("name"),
    REDEMPTION_ID("redemption_id"),
    CAMPAIGN_ID("campaign_id"),
    CAMPAIGN_NAME("campaign_name"),
    CUSTOMER_ID("customer_id"),
    TRANSACTION_REFERENCE("transaction_reference");

    override fun toString(): String = key
}
