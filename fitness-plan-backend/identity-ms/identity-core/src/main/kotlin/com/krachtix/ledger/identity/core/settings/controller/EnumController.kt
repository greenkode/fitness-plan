package com.krachtix.identity.core.settings.controller

import com.krachtix.identity.core.entity.CompanyRole
import com.krachtix.identity.core.entity.CompanySize
import com.krachtix.identity.core.entity.FiscalMonth
import com.krachtix.identity.core.entity.IntendedPurpose
import com.krachtix.identity.core.settings.dto.EnumOptionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/merchant/options")
@Tag(name = "Merchant Options", description = "Reference data options for merchant forms")
class EnumController {

    @GetMapping("/company-roles")
    @Operation(summary = "Get company role options")
    fun getCompanyRoles(): List<EnumOptionResponse> {
        return CompanyRole.entries.map { role ->
            EnumOptionResponse(
                value = role.name,
                label = role.label,
                description = role.description
            )
        }
    }

    @GetMapping("/company-sizes")
    @Operation(summary = "Get company size options")
    fun getCompanySizes(): List<EnumOptionResponse> {
        return CompanySize.entries.map { size ->
            EnumOptionResponse(
                value = size.name,
                label = size.label,
                description = size.description
            )
        }
    }

    @GetMapping("/industries")
    @Operation(summary = "Get industry options")
    fun getIndustries(): List<EnumOptionResponse> {
        return IntendedPurpose.entries.map { purpose ->
            EnumOptionResponse(
                value = purpose.name,
                label = purpose.label,
                description = purpose.description
            )
        }
    }

    @GetMapping("/fiscal-months")
    @Operation(summary = "Get fiscal year month options")
    fun getFiscalMonths(): List<EnumOptionResponse> {
        return FiscalMonth.entries.map { month ->
            EnumOptionResponse(
                value = month.code,
                label = month.label,
                description = month.description
            )
        }
    }
}
