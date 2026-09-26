package com.revana.bank.customer.exception;

public class CustomerNotFoundException
        extends RuntimeException {

    public CustomerNotFoundException(
            String message) {

        super(message);
    }
}