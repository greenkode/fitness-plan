package com.krachtix.enumeration

enum class ReasonCode(val code: String) {
    SUSPECTED_FRAUD("0001"),
    SECURITY_VIOLATION("0002"),
    MULTIPLE_INSUFFICIENT_FUNDS("0003"),
    MULTIPLE_TRANSFER_LIMIT_EXCEEDED("0004"),
    NON_COMPLIANCE_WITH_OPERATING_REGULATIONS("0005"),
    IDENTITY_THEFT("0006"),
    DUPLICATE_TRANSACTION_PROCESSING("0007"),
    FRAUDULENT_MULTIPLE_TRANSACTIONS("0008"),
    PAYMENT_MADE_BY_OTHER_MEANS("0009"),
    PURPOSE_OF_PAYMENT_NOT_REDEEMED("0010"),
    RECURRING_TRANSACTIONS("0011"),
    OTHERS("1111"),
    PLACED_BY_ADMIN("0000"),
    ERRONEOUS_INFLOW("0012"),
    REQUEST_FROM_OTHER_BANKS("0013"),
    FRAUDULENT_ACTIVITIES("0014");


    companion object {

        fun of(code: String) = values().find { it.code == code } ?: throw IllegalArgumentException("Invalid enum constant: $code")
    }
}
