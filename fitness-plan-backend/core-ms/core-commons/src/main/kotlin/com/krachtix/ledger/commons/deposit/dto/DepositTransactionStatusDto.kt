package com.krachtix.deposit.dto

enum class DepositTransactionStatus {
    CREATED,
    PENDING,
    PENDING_APPROVAL,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED
}
