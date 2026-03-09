package com.krachtix.commons.process.enumeration

import kotlin.collections.firstOrNull
import kotlin.text.equals

const val OTP_STATE_MACHINE = "OtpStateMachineFactory"
const val TRANSACTION_STATE_MACHINE = "TransactionStateMachineFactory"
const val P2P_STATE_MACHINE = "P2PStateMachineFactory"
const val DEPOSIT_STATE_MACHINE = "DepositStateMachineFactory"
const val CHECKOUT_STATE_MACHINE = "CheckoutStateMachineFactory"
const val LIEN_STATE_MACHINE = "LienStateMachineFactory"
const val ACCOUNT_CREATION_MACHINE = "AccountCreationMachineFactory"
const val ACCOUNT_EVENT_MACHINE = "AccountEventMachineFactory"
const val AUTHORIZATION_REQUEST_MACHINE = "AuthorizationRequestMachineFactory"
const val FUNDING_SOURCE_LINKING_MACHINE = "FundingSourceLinkingMachineFactory"

enum class RequestContextAttributeName {
    CHANNEL
}

object ProcessStrategyBeanNames {
    const val DEFAULT_PROCESS_STRATEGY = "DefaultProcessStrategy"
    const val LIEN_PROCESS_STRATEGY = "LienProcessStrategy"
    const val ACCOUNT_CREATION_PROCESS_STRATEGY = "AccountCreationProcessStrategy"
    const val TRANSACTION_PROCESS_STRATEGY = "TransactionProcessStrategy"
    const val FUND_POOL_ACCOUNT_PROCESS_STRATEGY = "FundPoolAccountProcessStrategy"
    const val DEPOSIT_PROCESS_STRATEGY = "DepositProcessStrategy"
    const val CHART_IMPORT_PROCESS_STRATEGY = "ChartImportProcessStrategy"
}

enum class ProcessType(val description: String, val timeInSeconds: Long, val strategyBeanName: String? = null) {
    DEFAULT("Default Process", -1, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    KNOWLEDGE_BASE_CREATION("Knowledge Base Creation Process", 86400, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    WEBHOOK_CREATION("Webhook Configuration Creation", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    WEBHOOK_UPDATE("Webhook Configuration Update", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    WEBHOOK_DELETION("Webhook Configuration Deletion", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    MERCHANT_USER_INVITATION("Merchant User Invitation", 604800, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    INVITATION_REVOCATION("Invitation Revocation", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    PASSWORD_RESET("Password Reset", 1200, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    TWO_FACTOR_AUTH("Two Factor Authentication", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    EMAIL_VERIFICATION("Email Verification", 86400, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    USER_REGISTRATION("User Registration", 86400, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    AVATAR_GENERATION("Avatar Generation Session", 1800, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    FRAUD_CHECK("Fraud Check Process", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    AUTH_REQUEST("Authentication Request", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    WITHDRAWAL("Withdrawal Process", 600, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    PROCESS("Generic Process", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    PROCESS_INITIATED("Process Initiated", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    DEPOSIT("Deposit Process", 600, ProcessStrategyBeanNames.DEPOSIT_PROCESS_STRATEGY),
    TRANSFER("Transfer Process", 600, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    FUNDING_SOURCE_LINKING("Funding Source Linking", 1800, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    ACCOUNT_CREATION("Account Creation Process", 600, ProcessStrategyBeanNames.ACCOUNT_CREATION_PROCESS_STRATEGY),
    LIEN_AMOUNT("Lien Account", -1, ProcessStrategyBeanNames.LIEN_PROCESS_STRATEGY),
    OTP("One Time Pin", 60, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    INTERNAL_TRANSFER("Internal Transfer", 1500, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    CHECKOUT_TRANSFER("Checkout Transfer", 1500, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    ACCOUNT_BLOCK("Account Block", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    ACCOUNT_UNBLOCK("Account Unblock", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    CONVERSION("Currency Conversion", 300, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    TRANSACTION_AUTHORIZATION("Transaction Authorization", 86400, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    SERVICE_AGREEMENT_SIGNING("License Agreement Signing", 86400, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    NAME_ENQUIRY("Name Enquiry", 300, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    EXCHANGE_RATE_QUOTE("Exchange Rate Quote", 300, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    CHART_IMPORT("Chart of Accounts Import", 600, ProcessStrategyBeanNames.CHART_IMPORT_PROCESS_STRATEGY),
    ORGANIZATION_SETUP("Organization Setup", 172800, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    TRANSACTION_TEMPLATE_SETUP("Transaction Template Setup", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    TEMPLATE_PRICING_CONFIGURATION("Template Pricing Configuration", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    ACCOUNT_LIMIT_CONFIGURATION("Account Limit Configuration", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    TEMPLATE_STATUS_CHANGE("Template Status Change", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    CAMPAIGN_CREATION("Campaign Creation", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    CAMPAIGN_UPDATE("Campaign Update", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    CAMPAIGN_DEACTIVATION("Campaign Deactivation", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    ACCOUNT_PROFILE_CREATION("Account Profile Creation", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    ACCOUNT_PROFILE_UPDATE("Account Profile Update", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    TOTP_SETUP("TOTP Authenticator Setup", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    TOTP_DISABLE("TOTP Authenticator Disable", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    PASSWORD_CHANGE("Password Change", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    ACCOUNT_PROFILE_SETUP("Account Profile Setup", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    CAMPAIGN_REDEMPTION("Campaign Reward Redemption", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    CAMPAIGN_EXPIRY("Campaign Reward Expiry", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    LEDGER_ADJUSTMENT("Ledger Adjustment", 600, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    CUSTOMER_CREATION("Customer Creation", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    CUSTOMER_UPDATE("Customer Update", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    POINTS_REDEMPTION("Points Redemption", 600, ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY),
    EXPORT_CONFIGURATION("Export Configuration", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    DATA_EXPORT("Data Export", 3600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    SCHEDULED_EXPORT("Scheduled Data Export", 3600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    BILLING_PLAN_CREATION("Billing Plan Creation", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    BILLING_PLAN_UPDATE("Billing Plan Update", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    BILLING_PLAN_DEACTIVATION("Billing Plan Deactivation", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    INVOICE_GENERATION("Invoice Generation", 3600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    STRIPE_SUBSCRIPTION_CREATION("Stripe Subscription Creation", 600, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    STRIPE_PAYMENT_RECEIVED("Stripe Payment Received", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    STRIPE_PAYMENT_FAILED("Stripe Payment Failed", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    MERCHANT_RESTRICTION_APPLIED("Merchant Restriction Applied", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
    MERCHANT_RESTRICTION_LIFTED("Merchant Restriction Lifted", 300, ProcessStrategyBeanNames.DEFAULT_PROCESS_STRATEGY),
}

enum class ProcessRequestType {
    CREATE_NEW_PROCESS,
    CUSTOMER_INFORMATION_UPDATE,
    EXPIRE_PROCESS,
    STATUS_CHECK_RETRY,
    MANUAL_RECONCILIATION,
    COMPLETE_PROCESS,
    FAIL_PROCESS,
    RESEND_AUTHENTICATION,
    UNLIEN_AMOUNT,
    LIEN_AMOUNT,
    FRAUD_CHECK,
    AUTH_REQUEST,
    FRAUD_CHECK_RESULT,
    EXECUTE_REMOTE_TRANSFER,
    FOLLOW_UP_TRANSACTION,
    AVATAR_PROMPT,
    AVATAR_REFINEMENT,
    ORGANIZATION_STEP_UPDATE
}

enum class ProcessRequestDataName(val description: String) {
    MERCHANT_ID("Merchant ID"),
    USER_IDENTIFIER("User Identifier"),
    AUTHENTICATION_REFERENCE("Authentication Reference"),
    DEVICE_FINGERPRINT("Device Fingerprint"),
    USER_EMAIL("User Email"),
    VERIFICATION_TOKEN("Verification Token"),
    AVATAR_PROMPT("Avatar Generation Prompt"),
    AVATAR_REFINED_PROMPT("Avatar Refined Prompt"),
    AVATAR_IMAGE_KEY("Avatar Image S3 Key"),
    AVATAR_PREVIOUS_IMAGE_KEY("Previous Avatar Image Key"),
    KNOWLEDGE_BASE_ID("Knowledge Base Id"),
    KNOWLEDGE_BASE_NAME("Knowledge Base Name"),
    ORGANIZATION_ID("Organization Id"),
    AMOUNT("Transaction Amount"),
    NARRATION("Transaction Narration"),
    INTEGRATOR_ID("Integrator ID"),
    TRANSACTION_TYPE("Transaction Type"),
    SENDER_NAME("Sender Name"),
    SENDER_ACCOUNT_ADDRESS("Sender Account Address"),
    SENDER_ACCOUNT_ID("Sender Account ID"),
    SENDER_INSTITUTION("Sender Institution"),
    RECIPIENT_ACCOUNT_ID("Recipient Account ID"),
    RECIPIENT_ACCOUNT_ADDRESS("Recipient Account Address"),
    EXTERNAL_REFERENCE("External Reference"),
    BENEFICIARY_ID("Beneficiary ID"),
    WEBHOOK_URL("Webhook URL"),
    CURRENCY("Currency"),
    CURRENCY_ISSUER("Currency Issuer"),
    CUSTOMER_ID("Customer ID"),
    ACCOUNT_ADDRESS("Account Address"),
    ACCOUNT_NAME("Account Name"),
    ADDRESS_TYPE("Address Type"),
    PROCESS_ID("Process ID"),
    ACCOUNT_PUBLIC_ID("Account Public ID"),
    EXCHANGE_RATE("Exchange Rate"),
    EXTERNAL_INSTITUTION_ID("External Institution ID"),
    EXTERNAL_INSTITUTION_NAME("External Institution Name"),
    REMOTE_TRANSACTION_REFERENCE("Remote Transaction Reference"),
    OTP("One Time Pin"),
    ENTERED_OTP("OTP Entered"),
    SENDER_CUSTOMER_ID("Sender Customer Id"),
    RECIPIENT_CUSTOMER_ID("Recipient Customer Id"),
    PAYMENT_METHOD("Payment Method"),
    TRANSACTION_DETAILS("Transaction Details"),
    TRANSACTION_REFERENCE("Transaction Reference"),
    EXCHANGE_RATE_QUOTE("Exchange rate quote details"),
    EXCHANGE_RATE_QUOTE_ID("Exchange rate quote id"),
    FRAUD_CHECK_RESULT("Fraud Check Result"),
    FIAT_SELL_AMOUNT("Fiat sell amount"),
    COUNTRY_CODE("Country Code"),
    RECEIVER_ENTITY_CONFIG_ID("Receiver entity config id"),
    REQUEST_DATA("Request Data"),
    ALIAS("Alias"),
    RECIPIENT_NAME("Recipient Name"),
    INTEGRATOR_NAME("Integrator Name"),
    REASON("Reason"),
    RECIPIENT_INSTITUTION("Recipient Institution"),
    AUTHORIZATION_REFERENCE("Authorization Reference"),
    PHONE_NUMBER("Phone Number"),
    EMAIL("Email"),
    BANK_CODE("Bank Code"),
    ACCOUNT_NUMBER("Account Number"),
    ADDITIONAL_PARAMETERS("Additional Parameters"),
    ADDRESS("Address"),
    INTEGRATOR_REFERENCE("Integrator Reference"),
    FUNDING_SOURCE_LINK_TYPE("Funding Source Link Type"),
    RETURN_URL("Return URL"),
    NOTIFY_URL("Notify URL"),
    FUNDING_SOURCE_ID("Funding Source Id"),
    AUTHORIZATION_TYPE("Authorization Type"),
    REQUEST_REFERENCE("Request Reference"),
    NAME_ENQUIRY_REFERENCE("Name Enquiry Reference"),
    MANDATE_REFERENCE("Mandate Reference"),
    MANDATE_BILLER_ID("Mandate Biller ID"),
    TRUST_LEVEL("Trust Level"),
    SENDER_EXTERNAL_ID("Sender External ID"),
    RECIPIENT_EXTERNAL_ID("Recipient External ID"),
    RECIPIENT_TRUST_LEVEL("Recipient Trust Level"),
    FUNDING_POOL_ACCOUNT_ID("Funding Pool Account"),
    FUNDING_SOURCE_TYPE("Funding Source Type"),
    CHART_CODE("Chart of Accounts Code"),
    CHART_ID("Chart of Accounts ID"),
    TEMPLATE_ID("Template ID"),
    TEMPLATE_NAME("Template Name"),
    ACCOUNTS_CREATED("Number of Accounts Created"),
    LAYERS_CREATED("Number of Layers Created"),
    JOURNAL_NAME("Journal Name"),
    COMPANY_NAME("Company Name"),
    INTENDED_PURPOSE("Intended Purpose"),
    COMPANY_SIZE("Company Size"),
    ROLE_IN_COMPANY("Role in Company"),
    COUNTRY("Country"),
    PHONE_NUMBER_RAW("Phone Number Raw"),
    WEBSITE_URL("Website URL"),
    SETUP_STEP("Setup Step"),
    DEFAULT_CURRENCY("Default Currency"),
    MULTI_CURRENCY_ENABLED("Multi-Currency Enabled"),
    CHART_TEMPLATE_ID("Chart Template ID"),
    FISCAL_YEAR_START("Fiscal Year Start"),
    TIMEZONE("Timezone"),
    DATE_FORMAT("Date Format"),
    NUMBER_FORMAT("Number Format"),
    ADDITIONAL_CURRENCIES("Additional Currencies"),
    CAMPAIGN_ID("Campaign ID"),
    CAMPAIGN_NAME("Campaign Name"),
    CAMPAIGN_TYPE("Campaign Type"),
    TWO_FACTOR_METHOD("Two Factor Authentication Method"),
    PROFILE_ID("Profile ID"),
    CAMPAIGN_AMOUNT("Campaign Reward Amount"),
    ORIGINAL_TRANSACTION_REFERENCE("Original Transaction Reference"),
    REDEMPTION_ID("Redemption ID"),
    EXPIRY_AMOUNT("Expiry Clawback Amount"),
    REWARD_TRANSACTION_REFERENCE("Reward Transaction Reference"),
    CUSTOMER_BALANCE("Customer Balance at Expiry"),
    ADJUSTMENT_REASON("Adjustment Reason"),
    CUSTOMER_PUBLIC_ID("Customer Public ID"),
    CUSTOMER_NAME("Customer Name"),
    CUSTOMER_STATUS("Customer Status"),
    POINTS_AMOUNT("Points Amount"),
    MONETARY_AMOUNT("Monetary Amount"),
    EXCHANGE_RATE_USED("Exchange Rate Used"),
    EXPORT_FORMAT("Export Format"),
    EXPORT_PLATFORM("Export Platform"),
    EXPORT_DATA_TYPE("Export Data Type"),
    EXPORT_FILE_KEY("Export File S3 Key"),
    EXPORT_CONFIG_ID("Export Configuration ID"),
}

enum class PaymentAdditionalInformation {
    RETURN_URL,
    NOTIFY_URL
}

enum class ProcessStakeholderType {
    ACTOR_USER,
    ACTOR_CLIENT,
    FOR_USER,
    SENDER_USER,
    RECIPIENT_USER
}

enum class ProcessState(val description: String) {
    PENDING("Awaiting further action"),
    FAILED("Process has failed"),
    COMPLETE("Process completed successfully"),
    EXPIRED("Process expired due to timeout"),
    CANCELLED("Process was cancelled"),
    UNKNOWN("State is unknown"),
    INITIAL("Initial state"),
    REJECTED("Process was rejected")
}

enum class ProcessEvent(val description: String) {
    AUTH_SUCCEEDED("Authentication completed successfully"),
    REMOTE_PAYMENT_COMPLETED("Remote payment processing completed"),
    REMOTE_PAYMENT_RESULT("Remote payment result received"),
    PROCESS_EXPIRED("Process expired due to timeout"),
    PROCESS_FAILED("Process failed with error"),
    PROCESS_COMPLETED("Process completed successfully"),
    REVERSE_PENDING_FUNDS("Pending funds reversed"),
    REVERSE_TRANSACTION("Completed transaction reversed"),
    PENDING_TRANSACTION_STATUS_VERIFIED("Transaction status verified"),
    PROCESS_CREATED("Process created"),
    CREDIT_RATING_OFFERS_RECEIVED("Credit rating offers received"),
    STATUS_CHECK_FAILED("Status Check failed"),
    MANUAL_RECONCILIATION_CONFIRMED("Manual Reconciliation Confirmed"),
    AUTH_TOKEN_RESEND("Auth Token Resend"),
    LIEN_AMOUNT("Lien Amount"),
    UNLIEN_AMOUNT("Unlien Amount"),
    AUTH_REQUEST("Authentication Request"),
    FRAUD_CHECK_INITIATED("Fraud Check Initiated"),
    FRAUD_CHECK_COMPLETED("Fraud Check Completed"),
    AUTH_FAILED("Authentication Failed"),
    PROCESS_INITIATED("Process Initiated"),
    REMOTE_TRANSFER_INITIATED("Remote Transfer Initiated"),
    CUSTOMER_INFORMATION("Customer Information"),
    REMOTE_TRANSFER_COMPLETED("Remote Transfer Completed"),
    TRANSFER_FAILED("Transfer Failed"),
    CUSTOMER_INFO_REQUESTED("Customer Info Requested"),
    KYC_COMPLETE("KYC Complete"),
    ONCHAIN_FUNDS_RECEIVED("Onchain Funds Received"),
    OFFCHAIN_FUNDS_RECEIVED("Offchain Funds Received"),
    QUOTE_EXPIRED("Quote Expired"),
    PENDING_TRANSACTION_INITIATED("Pending Transaction Initiated"),
    ORGANIZATION_PROFILE_COMPLETED("Organization profile step completed"),
    ORGANIZATION_CURRENCY_SELECTED("Organization currency step completed"),
    ORGANIZATION_PREFERENCES_SAVED("Organization preferences step completed"),
    STRIPE_SUBSCRIPTION_CREATED("Stripe subscription created"),
    STRIPE_PAYMENT_RECEIVED("Stripe payment received"),
    STRIPE_PAYMENT_FAILED("Stripe payment failed"),
    MERCHANT_RESTRICTED("Merchant restricted due to overdue invoice"),
    MERCHANT_RESTRICTION_LIFTED("Merchant restriction lifted after payment"),
    INVOICE_ISSUED("Invoice issued to merchant"),
    INVOICE_MARKED_OVERDUE("Invoice marked as overdue"),
    INVOICE_MARKED_PAID("Invoice marked as paid")
}

enum class ProcessHeader {
    PROCESS_ID,
    EXTERNAL_REFERENCE,
    FUNDING_SOURCE_ID,
    PROCESS,
    PROCESS_REQUEST_ID,
    SENDER_ACCOUNT,
    RECIPIENT_ACCOUNT,
    AMOUNT
}

enum class ProcessAuthorizationType {
    PIN,
    OTP,
    EXTERNAL_OTP,
    UNKNOWN;

    companion object {
        fun of(dataValue: String): ProcessAuthorizationType {
            return entries.firstOrNull { it.name.equals(dataValue, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
