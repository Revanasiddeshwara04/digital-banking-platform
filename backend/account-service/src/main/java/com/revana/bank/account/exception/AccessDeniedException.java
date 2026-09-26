package com.revana.bank.account.exception;

public class AccessDeniedException
        extends RuntimeException {

    public AccessDeniedException(
            String message) {

        super(message);
    }
}