package com.krachtix.commons.enumeration

enum class LedgerEntryOffset(val code: Short) {

    PENDING_OFFSET(1000),
    CREDIT_ALLOWANCES_OFFSET(2000),
    ON_HOLD_OFFSET(3000)
}