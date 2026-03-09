package com.krachtix.enumeration

const val STELLAR_INTEGRATION_CODE = "a1ab1370-1611-4157-9176-14f8ff3aaac9"

enum class CryptoCurrencyPlatform(val integrationCode: String) {
    STELLAR(STELLAR_INTEGRATION_CODE);
}