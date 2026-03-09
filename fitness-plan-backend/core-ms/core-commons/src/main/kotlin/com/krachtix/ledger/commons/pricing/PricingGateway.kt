package com.krachtix.pricing

import com.krachtix.pricing.dto.PricingDto
import com.krachtix.pricing.dto.PricingParameterDto

interface PricingGateway {

    fun calculateFees(parameters: PricingParameterDto) : PricingDto?
}