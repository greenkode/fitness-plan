package com.krachtix.identity.core.billing.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.commons.payment.PaymentGateway
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/internal/payment")
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class InternalPaymentController(
    private val paymentGateway: PaymentGateway
) {

    @GetMapping("/invoice-url")
    @PreAuthorize("hasAuthority('SCOPE_internal:read')")
    fun getInvoicePaymentUrl(@RequestParam externalInvoiceId: String): InvoicePaymentUrlResponse {
        log.info { "Internal request for invoice payment URL: $externalInvoiceId" }
        val url = paymentGateway.getInvoicePaymentUrl(externalInvoiceId)
        return InvoicePaymentUrlResponse(url = url)
    }
}

data class InvoicePaymentUrlResponse(
    val url: String
)
