package com.krachtix.commons.exception

import java.util.UUID

abstract class LedgerServiceException protected constructor(
    message: String,
    val responseCode: ResponseCode = ResponseCode.GENERAL_ERROR,
    val parameters: Array<out String> = emptyArray(),
) : RuntimeException(message) {

    val code: ExceptionCodeEnum = ExceptionCodeEnum.AN_ERROR_OCCURRED
}

class ProcessServiceException(
    message: String,
    responseCode: ResponseCode = ResponseCode.GENERAL_ERROR,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class BankingIntegrationException(
    message: String,
    responseCode: ResponseCode = ResponseCode.GENERAL_ERROR,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class TransactionServiceException(
    message: String,
    responseCode: ResponseCode = ResponseCode.TRANSACTION_FAILED,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class TransactionProcessingException(
    message: String,
    responseCode: ResponseCode = ResponseCode.TRANSACTION_FAILED,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class RecordNotFoundException(
    message: String,
    responseCode: ResponseCode = ResponseCode.UNABLE_TO_LOCATE_RECORD,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class DuplicateRecordException(
    message: String,
    responseCode: ResponseCode = ResponseCode.DUPLICATE_TRANSACTION_REF,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class InvalidRequestException(
    message: String,
    responseCode: ResponseCode = ResponseCode.INVALID_TRANSACTION,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class PricingServiceException(
    message: String,
    responseCode: ResponseCode = ResponseCode.PRICING_ERROR,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class AccountServiceException(
    message: String,
    responseCode: ResponseCode = ResponseCode.INVALID_ACCOUNT,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class IntegrationException(
    message: String,
    responseCode: ResponseCode = ResponseCode.GENERAL_ERROR,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class BankingServiceException(
    message: String,
    responseCode: ResponseCode = ResponseCode.GENERAL_ERROR,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class IllegalProcessDataException(
    message: String,
    responseCode: ResponseCode = ResponseCode.INVALID_TRANSACTION,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class FundingSourceAuthorizationRequiredException(
    val title: String,
    message: String,
    val reference: UUID,
    val pinLength: Int,
    responseCode: ResponseCode = ResponseCode.TRANSACTION_NOT_PERMITTED, vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class InvalidCredentialsException(
    message: String,
    responseCode: ResponseCode = ResponseCode.INVALID_CREDENTIALS,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class TwoFactorSessionInvalidException(
    message: String,
    responseCode: ResponseCode = ResponseCode.TWO_FACTOR_SESSION_INVALID,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class TwoFactorCodeInvalidException(
    message: String,
    responseCode: ResponseCode = ResponseCode.TWO_FACTOR_CODE_INVALID,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class TwoFactorMaxAttemptsException(
    message: String,
    responseCode: ResponseCode = ResponseCode.TWO_FACTOR_MAX_ATTEMPTS,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class TwoFactorAuthenticationRequiredException(
    val sessionId: String,
    message: String,
    val twoFactorMethod: String = "EMAIL",
    val restrictedToken: String? = null,
    responseCode: ResponseCode = ResponseCode.TWO_FACTOR_REQUIRED,
    vararg parameters: String,
) : LedgerServiceException(message, responseCode, parameters)

class EmailNotVerifiedException(
    message: String,
    responseCode: ResponseCode = ResponseCode.EMAIL_NOT_VERIFIED,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class TransactionLimitExceededException(
    message: String,
    responseCode: ResponseCode = ResponseCode.TRANSACTION_LIMIT_EXCEEDED,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class OrganizationNotSetupException(
    message: String,
    responseCode: ResponseCode = ResponseCode.ORGANIZATION_SETUP_REQUIRED,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class SrServiceException(
    message: String,
    responseCode: ResponseCode = ResponseCode.GENERAL_ERROR,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)

class InsufficientBalanceException(
    message: String,
    responseCode: ResponseCode = ResponseCode.INSUFFICIENT_FUNDS,
    vararg parameters: String
) : LedgerServiceException(message, responseCode, parameters)