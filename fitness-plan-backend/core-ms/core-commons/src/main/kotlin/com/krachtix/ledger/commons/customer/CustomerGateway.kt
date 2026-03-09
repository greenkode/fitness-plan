package com.krachtix.commons.customer

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.UUID

interface CustomerGateway {

    fun createCustomer(payload: CreateCustomerPayload): CustomerDto

    fun updateCustomer(publicId: UUID, payload: UpdateCustomerPayload): CustomerDto

    fun findByPublicId(publicId: UUID): CustomerDto?

    fun searchCustomers(search: String?, status: CustomerStatus?, pageable: Pageable): Page<CustomerDto>

    fun getCustomerSummary(startDate: Instant?, endDate: Instant?): CustomerSummaryDto

    fun getDailyCustomerCreation(startDate: Instant, endDate: Instant): List<DailyCountDto>
}
