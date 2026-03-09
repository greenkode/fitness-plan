package com.krachtix.identity.commons.payment

import java.io.Serializable
import java.time.Instant

data class PaymentCustomerDto(
    val externalCustomerId: String
) : Serializable

data class PaymentSubscriptionDto(
    val externalSubscriptionId: String,
    val status: String,
    val currentPeriodEnd: Instant
) : Serializable
