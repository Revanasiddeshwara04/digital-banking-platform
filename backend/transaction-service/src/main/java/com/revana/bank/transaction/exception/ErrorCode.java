package com.revana.bank.transaction.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Typed error codes covering every failure scenario defined in the
 * Fund Transfer requirements.
 *
 * Each constant carries:
 *  - a default human-readable message (surfaced in the error response body)
 *  - the HTTP status that best describes the failure class
 *
 * Using an enum keeps error codes consistent across service, exception,
 * handler, and tests — no magic strings.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INSUFFICIENT_BALANCE(
            "Insufficient balance in source account",
            HttpStatus.UNPROCESSABLE_ENTITY),

    ACCOUNT_NOT_FOUND(
            "Account not found",
            HttpStatus.NOT_FOUND),

    ACCOUNT_INACTIVE(
            "Account is not active",
            HttpStatus.UNPROCESSABLE_ENTITY),

    DAILY_LIMIT_EXCEEDED(
            "Daily transfer limit of \u20B95,00,000 exceeded",
            HttpStatus.UNPROCESSABLE_ENTITY),

    INVALID_TRANSFER_AMOUNT(
            "Transfer amount must be greater than zero",
            HttpStatus.BAD_REQUEST),

    SELF_TRANSFER_NOT_ALLOWED(
            "Source and destination accounts cannot be the same",
            HttpStatus.BAD_REQUEST),

    TRANSACTION_NOT_FOUND(
            "Transaction not found",
            HttpStatus.NOT_FOUND),

    TRANSFER_PROCESSING_FAILED(
            "Transfer could not be processed",
            HttpStatus.INTERNAL_SERVER_ERROR);

    /** Default message included in the error response body. */
    private final String message;

    /** HTTP status code returned to the caller. */
    private final HttpStatus httpStatus;
}
