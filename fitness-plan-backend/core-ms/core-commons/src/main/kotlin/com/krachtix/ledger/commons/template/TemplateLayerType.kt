package com.krachtix.commons.template

enum class TemplateLayerType(val offset: Short) {
    BASE(0),
    PENDING(1000),
    CREDIT_ALLOWANCES(2000),
    ON_HOLD(3000),
    DAILY_LIMIT(4000),
    CUMULATIVE_LIMIT(5000),
    FEE(6000),
    POINTS(7000)
}
