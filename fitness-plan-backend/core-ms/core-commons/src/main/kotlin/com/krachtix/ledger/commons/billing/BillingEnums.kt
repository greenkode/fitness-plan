package com.krachtix.commons.billing

enum class BillingCycle {
    MONTHLY,
    QUARTERLY,
    ANNUAL
}

enum class BillingPlanStatus {
    ACTIVE,
    SUSPENDED,
    CANCELLED
}

enum class InvoiceStatus {
    DRAFT,
    ISSUED,
    PAID,
    OVERDUE,
    CANCELLED
}

enum class PaymentEventType {
    INVOICE_PAID,
    INVOICE_PAYMENT_FAILED,
    SUBSCRIPTION_CREATED,
    SUBSCRIPTION_UPDATED,
    SUBSCRIPTION_CANCELLED,
    SUBSCRIPTION_PAST_DUE
}
