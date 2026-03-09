package com.krachtix.identity.core.entity

enum class CompanySize(
    val label: String,
    val description: String
) {
    SIZE_1_10("1-10 employees", "Startup or small team"),
    SIZE_11_50("11-50 employees", "Small business"),
    SIZE_51_200("51-200 employees", "Medium business"),
    SIZE_201_500("201-500 employees", "Large business"),
    SIZE_501_1000("501-1000 employees", "Enterprise"),
    SIZE_1001_PLUS("1001+ employees", "Large enterprise")
}
