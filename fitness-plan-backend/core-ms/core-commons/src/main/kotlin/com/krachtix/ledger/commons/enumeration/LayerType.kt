package com.krachtix.commons.enumeration

enum class LayerType(val offset: Short) {
    PENDING(1000),
    ON_HOLD(3000),
    CREDIT_ALLOWANCES(2000),
    FEE(6000)
}