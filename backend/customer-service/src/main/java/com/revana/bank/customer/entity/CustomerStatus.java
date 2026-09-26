package com.revana.bank.customer.entity;

public enum CustomerStatus {

    /**
     * Customer created but KYC not completed
     */
    PENDING_KYC,

    /**
     * Customer fully verified and active
     */
    ACTIVE,


    INACTIVE,

    /**
     * Temporarily blocked by bank
     */
    BLOCKED,

    /**
     * Customer profile closed
     */
    CLOSED
}