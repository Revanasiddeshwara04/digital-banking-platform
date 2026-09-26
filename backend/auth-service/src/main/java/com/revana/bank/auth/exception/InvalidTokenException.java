package com.revana.bank.auth.exception;

public class InvalidTokenException
        extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}