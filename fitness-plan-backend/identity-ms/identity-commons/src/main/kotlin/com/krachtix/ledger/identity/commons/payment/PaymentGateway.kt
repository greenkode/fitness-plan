package com.krachtix.identity.commons.payment

import java.util.UUID

interface PaymentGateway {
    fun createCustomer(merchantId: UUID, email: String, name: String): PaymentCustomerDto
    fun createSubscription(customerId: String, priceId: String): PaymentSubscriptionDto
    fun cancelSubscription(subscriptionId: String)
    fun getInvoicePaymentUrl(externalInvoiceId: String): String
    fun syncSubscriptionStatus(subscriptionId: String): PaymentSubscriptionDto
}
