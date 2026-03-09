package com.krachtix.identity.core.registration.controller

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class RegisterUserRequest(
    @field:NotBlank(message = "{registration.validation.email_required}")
    @field:Email(message = "{registration.validation.email_invalid}")
    val email: String,

    @field:NotBlank(message = "{registration.validation.password_required}")
    @field:Size(min = 8, message = "{registration.validation.password_min_length}")
    val password: String,

    @field:NotBlank(message = "{registration.validation.full_name_required}")
    val fullName: String,

    @field:NotBlank(message = "{registration.validation.organization_name_required}")
    val organizationName: String
)

data class RegisterUserResponse(
    val success: Boolean,
    val message: String,
    val userId: UUID? = null,
    val organizationId: UUID? = null,
    val isNewOrganization: Boolean = false
)

data class VerifyEmailRequest(
    val token: String
)

data class VerifyEmailResponse(
    val success: Boolean,
    val message: String,
    val userId: UUID? = null
)

data class ResendVerificationRequest(
    val email: String
)

data class ResendVerificationResponse(
    val success: Boolean,
    val message: String
)
