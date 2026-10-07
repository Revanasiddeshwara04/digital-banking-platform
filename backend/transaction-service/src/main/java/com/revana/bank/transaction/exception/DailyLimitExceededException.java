package com.revana.bank.transaction.exception;

/**
 * Exception thrown when daily transfer limit of ₹5,00,000
 * is exceeded for a source account.
 */
public class DailyLimitExceededException extends RuntimeException {
    
    public DailyLimitExceededException() {
        super("Daily transfer limit of ₹5,00,000 exceeded");
    }
    
    public DailyLimitExceededException(String message) {
        super(message);
    }
    
    public DailyLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}