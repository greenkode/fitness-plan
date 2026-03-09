package com.krachtix.commons.customer

import java.math.BigDecimal
import java.time.LocalDate

data class DailyCountDto(
    val date: LocalDate,
    val count: Long
)

data class DailyVolumeDto(
    val date: LocalDate,
    val count: Long,
    val value: BigDecimal
)
