package com.revana.bank.transaction.entity;

/**
 * Represents all possible lifecycle states of a fund transfer.
 *
 * PENDING  — transfer has been initiated but not yet processed
 * SUCCESS  — transfer completed and both accounts updated
 * FAILED   — transfer could not be completed (validation failure, system error)
 * REVERSED — a previously successful transfer has been reversed
 */
public enum TransferStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REVERSED
}
