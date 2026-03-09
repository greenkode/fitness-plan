package com.krachtix.commons.customer

import java.time.Instant
import java.util.UUID

data class CustomerDto(
    val publicId: UUID,
    val externalId: String?,
    val status: CustomerStatus,
    val profile: String?,
    val createdAt: Instant?
)

data class CreateCustomerPayload(
    val externalId: String?,
    val status: CustomerStatus = CustomerStatus.ACTIVE,
    val profile: String?,
    val publicId: UUID? = null
)

data class UpdateCustomerPayload(
    val externalId: String?,
    val status: CustomerStatus?,
    val profile: String?
)

data class CustomerSummaryDto(
    val totalCustomers: Long,
    val activeCustomers: Long,
    val newThisPeriod: Long,
    val suspendedCustomers: Long
)
