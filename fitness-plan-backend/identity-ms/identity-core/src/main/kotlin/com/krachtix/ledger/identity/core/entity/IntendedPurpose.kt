package com.krachtix.identity.core.entity

enum class IntendedPurpose(
    val label: String,
    val description: String
) {
    RETAIL("Retail & E-commerce", "Online and physical retail businesses"),
    FOOD_BEVERAGE("Food & Beverage", "Restaurants, cafes, and food services"),
    HOSPITALITY("Hospitality & Tourism", "Hotels, travel, and tourism services"),
    HEALTHCARE("Healthcare", "Medical and health services"),
    PROFESSIONAL_SERVICES("Professional Services", "Consulting, legal, and advisory services"),
    TECHNOLOGY("Technology", "Software, IT, and tech companies"),
    MANUFACTURING("Manufacturing", "Production and manufacturing businesses"),
    REAL_ESTATE("Real Estate", "Property management and real estate"),
    FINANCIAL_SERVICES("Financial Services", "Banking, insurance, and fintech"),
    OTHER("Other", "Other industry not listed above")
}
