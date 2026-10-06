package com.revana.bank.transaction.exception;

import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Centralised exception-to-HTTP mapping for the transaction-service.
 *
 * Handler precedence (most specific → least specific):
 *   1. TransferException         — business rule violations
 *   2. MethodArgumentNotValidException — @Valid failures on request bodies
 *   3. Exception                 — unexpected errors (5xx safety net)
 *
 * All responses share the same {@link ErrorResponse} envelope so API
 * clients can rely on a stable error schema regardless of failure type.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Business rule violations ──────────────────────────────────────────

    @ExceptionHandler(TransferException.class)
    public ResponseEntity<ErrorResponse> handleTransferException(TransferException ex) {
        log.warn("Transfer business rule violated [{}]: {}",
                ex.getErrorCode(), ex.getMessage());

        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(ex.getErrorCode().name(), ex.getMessage()));
    }

    // ── @Valid / @Validated failures ──────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Request validation failed: {}", message);

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of("VALIDATION_ERROR", message));
    }

    // ── Safety-net for unexpected errors ──────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error in transaction-service", ex);

        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of("INTERNAL_ERROR",
                        "An unexpected error occurred. Please try again later."));
    }

    // ── Error response envelope ───────────────────────────────────────────

    /**
     * Stable JSON error body returned for every failure type:
     * <pre>
     * {
     *   "errorCode"  : "INSUFFICIENT_BALANCE",
     *   "message"    : "Insufficient balance in source account",
     *   "timestamp"  : "2026-10-06T12:34:56"
     * }
     * </pre>
     */
    @Data
    @Builder
    public static class ErrorResponse {

        private String errorCode;
        private String message;
        private LocalDateTime timestamp;

        public static ErrorResponse of(String code, String message) {
            return ErrorResponse.builder()
                    .errorCode(code)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
}
