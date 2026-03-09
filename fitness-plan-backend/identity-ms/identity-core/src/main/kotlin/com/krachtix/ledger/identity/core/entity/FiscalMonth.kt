package com.krachtix.identity.core.entity

enum class FiscalMonth(
    val code: String,
    val label: String,
    val description: String
) {
    JANUARY("01", "January", "Start of Q1"),
    FEBRUARY("02", "February", "Start of fiscal year in February"),
    MARCH("03", "March", "Start of fiscal year in March"),
    APRIL("04", "April", "Common fiscal start for UK/India"),
    MAY("05", "May", "Start of fiscal year in May"),
    JUNE("06", "June", "Start of fiscal year in June"),
    JULY("07", "July", "Common fiscal start for Australia"),
    AUGUST("08", "August", "Start of fiscal year in August"),
    SEPTEMBER("09", "September", "Start of fiscal year in September"),
    OCTOBER("10", "October", "Common fiscal start for US federal"),
    NOVEMBER("11", "November", "Start of fiscal year in November"),
    DECEMBER("12", "December", "Start of fiscal year in December")
}
