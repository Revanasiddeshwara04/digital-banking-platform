package com.revana.bank.account.exception;

public class RateLimitExceededException
        extends RuntimeException {

    public RateLimitExceededException(
            String message) {

        super(message);
    }
}