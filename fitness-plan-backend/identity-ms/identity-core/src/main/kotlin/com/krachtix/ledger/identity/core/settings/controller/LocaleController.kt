package com.krachtix.identity.core.settings.controller

import com.krachtix.identity.core.settings.dto.DateFormatOption
import com.krachtix.identity.core.settings.dto.NumberFormatOption
import com.krachtix.identity.core.settings.dto.TimezoneGroupResponse
import com.krachtix.identity.core.settings.dto.TimezoneResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

@RestController
@RequestMapping("/locale")
@Tag(name = "Locale", description = "Timezone and locale options")
class LocaleController {

    @GetMapping("/timezones")
    @Operation(summary = "Get all available timezones grouped by region")
    fun getTimezones(): List<TimezoneGroupResponse> {
        val now = ZonedDateTime.now()

        return ZoneId.getAvailableZoneIds()
            .filter { it.contains("/") && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
            .map { zoneIdStr ->
                val zoneId = ZoneId.of(zoneIdStr)
                val offset = now.withZoneSameInstant(zoneId).offset
                val parts = zoneIdStr.split("/", limit = 2)
                val region = parts[0]
                val city = parts.getOrElse(1) { zoneIdStr }.replace("_", " ")

                Triple(region, zoneIdStr, TimezoneResponse(
                    id = zoneIdStr,
                    displayName = "$city (${offset.id})",
                    offset = offset.id
                ))
            }
            .groupBy { it.first }
            .map { (region, timezones) ->
                TimezoneGroupResponse(
                    region = region,
                    timezones = timezones
                        .map { it.third }
                        .sortedBy { it.displayName }
                )
            }
            .sortedBy { it.region }
    }

    @GetMapping("/timezones/flat")
    @Operation(summary = "Get all available timezones as a flat list")
    fun getTimezonesFlatList(): List<TimezoneResponse> {
        val now = ZonedDateTime.now()

        return ZoneId.getAvailableZoneIds()
            .filter { it.contains("/") && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
            .map { zoneIdStr ->
                val zoneId = ZoneId.of(zoneIdStr)
                val offset = now.withZoneSameInstant(zoneId).offset
                val parts = zoneIdStr.split("/", limit = 2)
                val city = parts.getOrElse(1) { zoneIdStr }.replace("_", " ")

                TimezoneResponse(
                    id = zoneIdStr,
                    displayName = "$city (${offset.id})",
                    offset = offset.id
                )
            }
            .sortedWith(compareBy({ it.offset }, { it.displayName }))
    }

    @GetMapping("/date-formats")
    @Operation(summary = "Get available date format options")
    fun getDateFormats(): List<DateFormatOption> {
        return listOf(
            DateFormatOption("MM/DD/YYYY", "MM/DD/YYYY", "02/05/2026"),
            DateFormatOption("DD/MM/YYYY", "DD/MM/YYYY", "05/02/2026"),
            DateFormatOption("YYYY-MM-DD", "YYYY-MM-DD (ISO)", "2026-02-05"),
            DateFormatOption("DD.MM.YYYY", "DD.MM.YYYY", "05.02.2026"),
            DateFormatOption("DD-MM-YYYY", "DD-MM-YYYY", "05-02-2026"),
            DateFormatOption("MMM DD, YYYY", "MMM DD, YYYY", "Feb 05, 2026"),
            DateFormatOption("DD MMM YYYY", "DD MMM YYYY", "05 Feb 2026")
        )
    }

    @GetMapping("/number-formats")
    @Operation(summary = "Get available number format options")
    fun getNumberFormats(): List<NumberFormatOption> {
        return listOf(
            NumberFormatOption("1,234.56", "1,234.56 (US/UK)", "1,234,567.89"),
            NumberFormatOption("1.234,56", "1.234,56 (Europe)", "1.234.567,89"),
            NumberFormatOption("1 234.56", "1 234.56 (Space separator)", "1 234 567.89"),
            NumberFormatOption("1 234,56", "1 234,56 (Space + comma)", "1 234 567,89")
        )
    }
}
