package com.revana.bank.customer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Customer Not Found
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String, Object>>
    handleCustomerNotFoundException(
            CustomerNotFoundException ex) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("timestamp",
                LocalDateTime.now());

        response.put("status",
                HttpStatus.NOT_FOUND.value());

        response.put("error",
                "Customer Not Found");

        response.put("message",
                ex.getMessage());

        return ResponseEntity.status(
                        HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Validation Errors
     */
    @ExceptionHandler(
            MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>
    handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {

                    String field =
                            ((FieldError) error)
                                    .getField();

                    String message =
                            error.getDefaultMessage();

                    validationErrors.put(
                            field,
                            message);
                });

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("timestamp",
                LocalDateTime.now());

        response.put("status",
                HttpStatus.BAD_REQUEST.value());

        response.put("error",
                "Validation Failed");

        response.put("errors",
                validationErrors);

        return ResponseEntity.badRequest()
                .body(response);
    }

    /**
     * Business Errors
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>>
    handleRuntimeException(
            RuntimeException ex) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("timestamp",
                LocalDateTime.now());

        response.put("status",
                HttpStatus.BAD_REQUEST.value());

        response.put("error",
                "Business Validation Error");

        response.put("message",
                ex.getMessage());

        return ResponseEntity.badRequest()
                .body(response);
    }

    /**
     * Unexpected Errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleException(
            Exception ex) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("timestamp",
                LocalDateTime.now());

        response.put("status",
                HttpStatus.INTERNAL_SERVER_ERROR.value());

        response.put("error",
                "Internal Server Error");

        response.put("message",
                ex.getMessage());

        return ResponseEntity.status(
                        HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}