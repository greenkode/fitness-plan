package com.krachtix.identity.stripe

import com.stripe.model.Customer
import com.stripe.model.Invoice
import com.stripe.model.Subscription
import com.stripe.param.CustomerCreateParams
import com.stripe.param.SubscriptionCancelParams
import com.stripe.param.SubscriptionCreateParams
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.identity.commons.payment.PaymentCustomerDto
import com.krachtix.identity.commons.payment.PaymentGateway
import com.krachtix.identity.commons.payment.PaymentSubscriptionDto
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class StripePaymentService : PaymentGateway {

    override fun createCustomer(merchantId: UUID, email: String, name: String): PaymentCustomerDto {
        log.info { "Creating Stripe customer for merchant=$merchantId" }

        val params = CustomerCreateParams.builder()
            .setEmail(email)
            .setName(name)
            .putMetadata("merchant_id", merchantId.toString())
            .build()

        val customer = Customer.create(params)
        log.info { "Created Stripe customer ${customer.id} for merchant=$merchantId" }

        return PaymentCustomerDto(externalCustomerId = customer.id)
    }

    override fun createSubscription(customerId: String, priceId: String): PaymentSubscriptionDto {
        log.info { "Creating Stripe subscription for customer=$customerId, price=$priceId" }

        val params = SubscriptionCreateParams.builder()
            .setCustomer(customerId)
            .addItem(
                SubscriptionCreateParams.Item.builder()
                    .setPrice(priceId)
                    .build()
            )
            .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
            .setCollectionMethod(SubscriptionCreateParams.CollectionMethod.CHARGE_AUTOMATICALLY)
            .build()

        val subscription = Subscription.create(params)
        log.info { "Created Stripe subscription ${subscription.id} for customer=$customerId" }

        return PaymentSubscriptionDto(
            externalSubscriptionId = subscription.id,
            status = subscription.status,
            currentPeriodEnd = Instant.ofEpochSecond(subscription.currentPeriodEnd)
        )
    }

    override fun cancelSubscription(subscriptionId: String) {
        log.info { "Cancelling Stripe subscription $subscriptionId" }

        val subscription = Subscription.retrieve(subscriptionId)
        subscription.cancel(SubscriptionCancelParams.builder().build())

        log.info { "Cancelled Stripe subscription $subscriptionId" }
    }

    override fun getInvoicePaymentUrl(externalInvoiceId: String): String {
        log.info { "Retrieving Stripe invoice payment URL for invoice=$externalInvoiceId" }

        val invoice = Invoice.retrieve(externalInvoiceId)
        return invoice.hostedInvoiceUrl
            ?: throw IllegalStateException("No hosted invoice URL available for invoice $externalInvoiceId")
    }

    override fun syncSubscriptionStatus(subscriptionId: String): PaymentSubscriptionDto {
        log.debug { "Syncing Stripe subscription status for $subscriptionId" }

        val subscription = Subscription.retrieve(subscriptionId)

        return PaymentSubscriptionDto(
            externalSubscriptionId = subscription.id,
            status = subscription.status,
            currentPeriodEnd = Instant.ofEpochSecond(subscription.currentPeriodEnd)
        )
    }
}
