package com.revana.bank.transaction.exception;

import lombok.Getter;

/**
 * Base runtime exception for all fund-transfer business-rule violations.
 *
 * Every throw site must supply an {@link ErrorCode} so that
 * {@link GlobalExceptionHandler} can map the failure to the correct
 * HTTP status and structured error body without catching raw
 * RuntimeExceptions.
 *
 * Usage:
 *   throw new TransferException(ErrorCode.INSUFFICIENT_BALANCE);
 *   throw new TransferException(ErrorCode.ACCOUNT_NOT_FOUND, "ACC99999");
 */
@Getter
public class TransferException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * Use when the default message from the ErrorCode is sufficient.
     */
    public TransferException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * Use when extra context (e.g. the specific account number) should
     * be included in the error message.
     */
    public TransferException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
