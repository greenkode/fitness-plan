package com.krachtix.config

import com.krachtix.pricing.PricingGateway
import com.krachtix.pricing.dto.PricingDto
import com.krachtix.pricing.dto.PricingParameterDto
import org.javamoney.moneta.Money
import org.springframework.stereotype.Service

@Service
class DefaultPricingGateway : PricingGateway {

    override fun calculateFees(parameters: PricingParameterDto): PricingDto {
        val currency = parameters.amount.currency
        val zero = Money.zero(currency)

        return PricingDto(
            fee = zero,
            commission = zero,
            vat = zero,
            rebate = zero
        )
    }
}
