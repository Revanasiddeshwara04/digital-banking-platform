package com.revana.bank.transaction.exception;

/**
 * Exception thrown when source and destination accounts
 * are the same (self-transfer).
 */
public class SelfTransferNotAllowedException extends RuntimeException {
    
    public SelfTransferNotAllowedException() {
        super("Source and destination accounts cannot be the same");
    }
    
    public SelfTransferNotAllowedException(String message) {
        super(message);
    }
    
    public SelfTransferNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}