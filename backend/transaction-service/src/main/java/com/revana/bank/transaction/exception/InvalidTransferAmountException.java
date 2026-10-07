package com.revana.bank.transaction.exception;

/**
 * Exception thrown when transfer amount is invalid
 * (e.g., zero, negative, or null).
 */
public class InvalidTransferAmountException extends RuntimeException {
    
    public InvalidTransferAmountException() {
        super("Transfer amount must be greater than zero");
    }
    
    public InvalidTransferAmountException(String message) {
        super(message);
    }
    
    public InvalidTransferAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}