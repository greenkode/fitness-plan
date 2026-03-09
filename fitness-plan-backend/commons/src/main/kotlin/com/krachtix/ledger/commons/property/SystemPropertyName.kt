package com.krachtix.commons.property

enum class SystemPropertyName(val defaultValue: String) {
    TRANSACTION_STATISTICS_EMAIL_INTERVAL_HOURS("24"),
    TRANSACTION_AUTO_RETRY_PERIOD("300"),
    STRICT_ACCOUNT_CODES("false"),
    CHART_IMPORT_MAX_DEPTH("5"),
    CHART_IMPORT_MAX_FINAL_ACCOUNTS("50"),
    CHART_IMPORT_MAX_TOTAL_ACCOUNTS("200"),
    CHART_IMPORT_CUSTOM_ASYNC("true")
}