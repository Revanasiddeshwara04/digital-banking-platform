package com.revana.bank.transaction.exception;

/**
 * Exception thrown when source or destination account
 * cannot be found in the system.
 */
public class AccountNotFoundException extends RuntimeException {
    
    public AccountNotFoundException() {
        super("Account not found");
    }
    
    public AccountNotFoundException(String message) {
        super(message);
    }
    
    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}