package com.krachtix.identity.core.totp.api

import an.awesome.pipelinr.Pipeline
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import com.krachtix.identity.core.totp.command.ConfirmTotpSetupCommand
import com.krachtix.identity.core.totp.command.DisableTotpCommand
import com.krachtix.identity.core.totp.command.InitiateTotpSetupCommand
import com.krachtix.identity.core.totp.command.RegenerateRecoveryCodesCommand
import com.krachtix.identity.core.totp.query.GetTotpStatusQuery
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/auth/totp")
@Tag(name = "TOTP Authentication", description = "TOTP authenticator app management")
@SecurityRequirement(name = "bearerAuth")
class TotpController(
    private val pipeline: Pipeline
) {

    @GetMapping("/status")
    @Operation(summary = "Get TOTP status", description = "Check if TOTP is enabled and remaining recovery codes")
    fun getTotpStatus(): TotpStatusResponse {
        val result = pipeline.send(GetTotpStatusQuery())
        return TotpStatusResponse(
            totpEnabled = result.totpEnabled,
            remainingRecoveryCodes = result.remainingRecoveryCodes
        )
    }

    @PostMapping("/setup")
    @Operation(summary = "Initiate TOTP setup", description = "Generate TOTP secret and QR code URI")
    @RateLimiting(name = "totp-setup")
    fun initiateSetup(): TotpSetupResponse {
        log.info { "TOTP setup initiation requested" }
        val result = pipeline.send(InitiateTotpSetupCommand())
        return TotpSetupResponse(
            secret = result.secret,
            qrCodeUri = result.qrCodeUri,
            message = result.message
        )
    }

    @PostMapping("/setup/confirm")
    @Operation(summary = "Confirm TOTP setup", description = "Verify TOTP code and enable authenticator")
    @RateLimiting(name = "totp-verify")
    fun confirmSetup(@RequestBody request: TotpConfirmRequest): TotpConfirmResponse {
        log.info { "TOTP setup confirmation requested" }
        val result = pipeline.send(ConfirmTotpSetupCommand(code = request.code))
        return TotpConfirmResponse(
            recoveryCodes = result.recoveryCodes,
            message = result.message
        )
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable TOTP", description = "Disable TOTP authenticator with code or password verification")
    @RateLimiting(name = "totp-verify")
    fun disableTotp(@RequestBody request: TotpDisableRequest): TotpDisableResponse {
        log.info { "TOTP disable requested" }
        val result = pipeline.send(DisableTotpCommand(code = request.code, password = request.password))
        return TotpDisableResponse(message = result.message)
    }

    @PostMapping("/recovery-codes")
    @Operation(summary = "Regenerate recovery codes", description = "Generate new recovery codes after TOTP verification")
    @RateLimiting(name = "totp-verify")
    fun regenerateRecoveryCodes(@RequestBody request: TotpRegenerateRequest): TotpRegenerateResponse {
        log.info { "Recovery code regeneration requested" }
        val result = pipeline.send(RegenerateRecoveryCodesCommand(code = request.code))
        return TotpRegenerateResponse(
            recoveryCodes = result.recoveryCodes,
            message = result.message
        )
    }
}
