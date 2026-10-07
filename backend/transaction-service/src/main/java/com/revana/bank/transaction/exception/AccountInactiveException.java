package com.revana.bank.transaction.exception;

/**
 * Exception thrown when source or destination account
 * is not in ACTIVE status.
 */
public class AccountInactiveException extends RuntimeException {
    
    public AccountInactiveException() {
        super("Account is not active");
    }
    
    public AccountInactiveException(String message) {
        super(message);
    }
    
    public AccountInactiveException(String message, Throwable cause) {
        super(message, cause);
    }
}