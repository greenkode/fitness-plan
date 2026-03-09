package com.krachtix.identity.core.entity

enum class CompanyRole(
    val label: String,
    val description: String
) {
    CEO("CEO / Founder", "Chief Executive Officer or company founder"),
    CTO("CTO / Founder", "Chief Technology Officer or company founder"),
    CFO("CFO / Finance Lead", "Chief Financial Officer or finance leadership"),
    COO("COO / Operations Lead", "Chief Operations Officer or operations leadership"),
    FINANCE_MANAGER("Finance Manager", "Manages financial operations and reporting"),
    ACCOUNTANT("Accountant", "Handles accounting and financial records"),
    BOOKKEEPER("Bookkeeper", "Manages day-to-day financial transactions"),
    BUSINESS_OWNER("Business Owner", "Owner or partner of the business"),
    CONTROLLER("Controller", "Oversees accounting operations and financial reporting"),
    AUDITOR("Auditor", "Reviews and verifies financial records"),
    OTHER("Other", "Other role not listed above")
}
