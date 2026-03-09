package com.krachtix.commons.exception

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.exc.MismatchedInputException
import com.giffing.bucket4j.spring.boot.starter.context.RateLimitException
import io.github.oshai.kotlinlogging.KotlinLogging
import com.krachtix.commons.i18n.MessageService
import com.krachtix.commons.model.ErrorResponse
import com.krachtix.commons.process.enumeration.ProcessState
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import java.time.Instant

data class IdentityErrorResponse(
    val timestamp: String = Instant.now().toString(),
    val status: Int,
    val error: String,
    val message: String,
    val sessionId: String? = null,
    val twoFactorMethod: String? = null,
    val restrictedToken: String? = null,
    val path: String? = null
)

@RestControllerAdvice
class GlobalExceptionHandler(
    private val messageService: MessageService,
    private val objectMapper: ObjectMapper
) {

    private val log = KotlinLogging.logger { }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DuplicateRecordException::class)
    fun handleDuplicateRequest(ex: DuplicateRecordException): ErrorResponse {

        log.error(ex) { ex.message!! }

        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(LedgerServiceException::class)
    fun handleInvalidRequestException(ex: LedgerServiceException): ErrorResponse {

        log.error(ex) { ex.message!! }

        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    @ExceptionHandler(FundingSourceAuthorizationRequiredException::class)
    fun handleFundingSourceAuthorizationRequiredException(ex: FundingSourceAuthorizationRequiredException): Map<String, String> {

        return mapOf(
            "title" to ex.title,
            "message" to ex.message!!,
            "reference" to ex.reference.toString(),
            "pin_length" to ex.pinLength.toString(),
            "response_code" to ex.responseCode.code
        )
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleOptimisticLockingFailure(ex: ObjectOptimisticLockingFailureException): ErrorResponse {
        log.warn { "Concurrent modification detected: ${ex.message}" }

        return ErrorResponse(
            message = messageService.getMessage("commons.error.concurrent_modification"),
            errorCode = ResponseCode.GENERAL_ERROR,
            reference = null,
            status = ProcessState.UNKNOWN,
            entry = mapOf()
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUncaughtException(ex: Exception): ErrorResponse {

        log.error(ex) { ex.message }

        val message = (ex as? LedgerServiceException)?.message
            ?: messageService.getMessage("commons.error.internal")

        return ErrorResponse(
            message = message,
            errorCode = ResponseCode.GENERAL_ERROR,
            reference = null,
            status = ProcessState.UNKNOWN,
            entry = mapOf()
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDeniedException(ex: AccessDeniedException): ErrorResponse {
        log.error { ex }
        return ErrorResponse(
            message = messageService.getMessage("commons.error.access_denied"),
            errorCode = ResponseCode.ACCESS_DENIED,
            reference = "",
            status = ProcessState.UNKNOWN,
            entry = mapOf()
        )
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ErrorResponse {
        (ex as? BadCredentialsException ?: ex as? CredentialsExpiredException)
            ?.let { log.error { it.message } }
            ?: log.error { ex }

        return ErrorResponse(
            message = messageService.getMessage("commons.error.authentication_failed"),
            errorCode = ResponseCode.AUTHENTICATION_FAILED,
            reference = "",
            status = ProcessState.UNKNOWN,
            entry = mapOf()
        )
    }


    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(RecordNotFoundException::class)
    fun handleRecordNotFoundException(ex: RecordNotFoundException): ErrorResponse {

        log.error(ex) { ex.message!! }

        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ProcessServiceException::class)
    fun handleProcessServiceException(ex: ProcessServiceException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(BankingIntegrationException::class)
    fun handleBankingIntegrationException(ex: BankingIntegrationException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(TransactionServiceException::class)
    fun handleTransactionServiceException(ex: TransactionServiceException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(TransactionProcessingException::class)
    fun handleTransactionProcessingException(ex: TransactionProcessingException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(ex: InvalidRequestException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ErrorResponse {
        log.warn { "Invalid request body: ${ex.message}" }

        val fieldName = (ex.cause as? MismatchedInputException)?.path
            ?.lastOrNull()?.propertyName

        val message = when {
            fieldName != null -> messageService.getMessage(
                "commons.error.field_required",
                fieldName.toSnakeCase()
            )
            else -> messageService.getMessage("commons.error.invalid_request_body")
        }

        return ErrorResponse(
            message = message,
            errorCode = ResponseCode.INVALID_REQUEST,
            reference = "",
            status = ProcessState.UNKNOWN,
            entry = fieldName?.let { mapOf("field" to it.toSnakeCase()) } ?: mapOf()
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        log.warn { "Validation failed: ${ex.message}" }

        val errors = ex.bindingResult.fieldErrors.associate { error ->
            error.field.toSnakeCase() to (error.defaultMessage ?: "Invalid value")
        }

        val response = mapOf(
            "message" to messageService.getMessage("commons.error.validation_failed"),
            "errors" to errors
        )

        return ResponseEntity.badRequest().body(response)
    }

    private fun String.toSnakeCase(): String {
        return this.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(PricingServiceException::class)
    fun handlePricingServiceException(ex: PricingServiceException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(AccountServiceException::class)
    fun handleAccountServiceException(ex: AccountServiceException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(IntegrationException::class)
    fun handleIntegrationException(ex: IntegrationException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(BankingServiceException::class)
    fun handleBankingServiceException(ex: BankingServiceException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalProcessDataException::class)
    fun handleIllegalProcessDataException(ex: IllegalProcessDataException): ErrorResponse {
        log.error(ex) { ex.message!! }
        return ErrorResponse(
            ex.message!!,
            ex.responseCode,
            "",
            status = ProcessState.UNKNOWN,
            mapOf("response_code" to ex.responseCode)
        )
    }

    @ExceptionHandler(SrServiceException::class)
    fun handleSrServiceException(ex: SrServiceException): ResponseEntity<IdentityErrorResponse> {
        log.warn { "Service exception: ${ex.message}" }

        val httpStatus = ex.responseCode.httpStatus
        val errorResponse = IdentityErrorResponse(
            status = httpStatus.value(),
            error = ex.responseCode.description,
            message = ex.message ?: messageService.getMessage("auth.error.internal")
        )

        return ResponseEntity.status(httpStatus).body(errorResponse)
    }

    @ExceptionHandler(TwoFactorAuthenticationRequiredException::class)
    fun handleTwoFactorAuthenticationRequiredException(
        ex: TwoFactorAuthenticationRequiredException
    ): ResponseEntity<IdentityErrorResponse> {
        log.info { "Two-factor authentication required for session: ${ex.sessionId}" }

        val errorResponse = IdentityErrorResponse(
            status = HttpStatus.PRECONDITION_REQUIRED.value(),
            error = "2FA_REQUIRED",
            message = ex.message ?: messageService.getMessage("twofactor.required"),
            sessionId = ex.sessionId,
            twoFactorMethod = ex.twoFactorMethod,
            restrictedToken = ex.restrictedToken
        )

        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(errorResponse)
    }

    @ExceptionHandler(OrganizationNotSetupException::class)
    fun handleOrganizationNotSetupException(ex: OrganizationNotSetupException): ResponseEntity<IdentityErrorResponse> {
        log.info { "Organization setup required: ${ex.message}" }

        val errorResponse = IdentityErrorResponse(
            status = HttpStatus.PRECONDITION_REQUIRED.value(),
            error = "ORGANIZATION_SETUP_REQUIRED",
            message = ex.message ?: messageService.getMessage("settings.error.setup_required")
        )

        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(errorResponse)
    }

    @ExceptionHandler(RateLimitException::class)
    fun handleRateLimitException(ex: RateLimitException): ResponseEntity<IdentityErrorResponse> {
        log.warn { "Rate limit exceeded: ${ex.message}" }

        val errorResponse = IdentityErrorResponse(
            status = HttpStatus.TOO_MANY_REQUESTS.value(),
            error = "Too Many Requests",
            message = messageService.getMessage("auth.error.rate_limit")
        )

        return ResponseEntity.status(
            HttpStatus.TOO_MANY_REQUESTS).body(errorResponse)
    }

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDeniedException(ex: AuthorizationDeniedException): ResponseEntity<IdentityErrorResponse> {
        log.warn { "Access denied: ${ex.message}" }

        val errorResponse = IdentityErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = "Forbidden",
            message = messageService.getMessage("auth.error.access_denied")
        )

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentialsException(ex: InvalidCredentialsException): ResponseEntity<IdentityErrorResponse> {
        log.warn { "Invalid credentials: ${ex.message}" }

        val errorResponse = IdentityErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "invalid_credentials",
            message = ex.message ?: messageService.getMessage("auth.error.invalid_credentials")
        )

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    @ExceptionHandler(LockedException::class)
    fun handleLockedException(ex: LockedException): ResponseEntity<IdentityErrorResponse> {
        log.warn { "Account locked: ${ex.message}" }

        val errorResponse = IdentityErrorResponse(
            status = HttpStatus.LOCKED.value(),
            error = "account_locked",
            message = ex.message ?: messageService.getMessage("auth.error.account_locked")
        )

        return ResponseEntity.status(HttpStatus.LOCKED).body(errorResponse)
    }

    @ExceptionHandler(HttpClientErrorException::class)
    fun handleHttpClientErrorException(ex: HttpClientErrorException): ResponseEntity<ErrorResponse> {
        log.error(ex) { "HTTP Client Error: ${ex.statusCode} - ${ex.responseBodyAsString}" }

        val message = extractErrorMessage(ex.responseBodyAsString)
            ?: ex.message
            ?: "External service returned an error"

        val errorResponse = ErrorResponse(
            message = message,
            errorCode = ResponseCode.EXTERNAL_SERVICE_ERROR,
            reference = null,
            status = ProcessState.UNKNOWN,
            entry = mapOf()
        )

        val responseStatus = when (ex.statusCode) {
            HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> HttpStatus.BAD_GATEWAY
            else -> ex.statusCode
        }

        return ResponseEntity.status(responseStatus).body(errorResponse)
    }

    @ExceptionHandler(HttpServerErrorException::class)
    fun handleHttpServerErrorException(ex: HttpServerErrorException): ResponseEntity<ErrorResponse> {
        log.error(ex) { "HTTP Server Error: ${ex.statusCode} - ${ex.responseBodyAsString}" }

        val message = extractErrorMessage(ex.responseBodyAsString)
            ?: "External service is temporarily unavailable"

        val errorResponse = ErrorResponse(
            message = message,
            errorCode = ResponseCode.EXTERNAL_SERVICE_ERROR,
            reference = null,
            status = ProcessState.UNKNOWN,
            entry = mapOf()
        )

        return ResponseEntity.status(ex.statusCode).body(errorResponse)
    }

    private fun extractErrorMessage(responseBody: String?): String? {
        if (responseBody.isNullOrBlank()) return null

        return runCatching {
            val json = objectMapper.readTree(responseBody)
            json.get("message")?.asText()
        }.getOrNull()
    }
}
