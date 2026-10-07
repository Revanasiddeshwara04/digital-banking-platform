package com.revana.bank.transaction.exception;

/**
 * Exception thrown when source account has insufficient balance
 * to complete the requested transfer.
 */
public class InsufficientBalanceException extends RuntimeException {
    
    public InsufficientBalanceException() {
        super("Insufficient balance in source account");
    }
    
    public InsufficientBalanceException(String message) {
        super(message);
    }
    
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}