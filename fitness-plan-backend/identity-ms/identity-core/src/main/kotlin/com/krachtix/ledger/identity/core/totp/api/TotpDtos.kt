package com.krachtix.identity.core.totp.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "TOTP status response")
data class TotpStatusResponse(
    @Schema(description = "Whether TOTP is enabled")
    val totpEnabled: Boolean,
    @Schema(description = "Number of remaining unused recovery codes")
    val remainingRecoveryCodes: Long
)

@Schema(description = "TOTP setup response")
data class TotpSetupResponse(
    @Schema(description = "Base32 encoded TOTP secret")
    val secret: String,
    @Schema(description = "otpauth:// URI for QR code generation")
    val qrCodeUri: String,
    @Schema(description = "Response message")
    val message: String
)

@Schema(description = "TOTP setup confirmation request")
data class TotpConfirmRequest(
    @Schema(description = "6-digit TOTP code from authenticator app", example = "123456")
    val code: String
)

@Schema(description = "TOTP setup confirmation response")
data class TotpConfirmResponse(
    @Schema(description = "Recovery codes for account recovery")
    val recoveryCodes: List<String>,
    @Schema(description = "Response message")
    val message: String
)

@Schema(description = "TOTP disable request")
data class TotpDisableRequest(
    @Schema(description = "6-digit TOTP code from authenticator app")
    val code: String? = null,
    @Schema(description = "User password for verification")
    val password: String? = null
)

@Schema(description = "TOTP disable response")
data class TotpDisableResponse(
    @Schema(description = "Response message")
    val message: String
)

@Schema(description = "Recovery codes regeneration request")
data class TotpRegenerateRequest(
    @Schema(description = "6-digit TOTP code from authenticator app", example = "123456")
    val code: String
)

@Schema(description = "Recovery codes regeneration response")
data class TotpRegenerateResponse(
    @Schema(description = "New recovery codes")
    val recoveryCodes: List<String>,
    @Schema(description = "Response message")
    val message: String
)
