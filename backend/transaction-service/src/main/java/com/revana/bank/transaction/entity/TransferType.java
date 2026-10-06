package com.revana.bank.transaction.entity;

/**
 * Payment rail / transfer mode supported by the platform.
 *
 * IMPS — Immediate Payment Service  (24x7, real-time, up to ₹5 lakh)
 * NEFT — National Electronic Funds Transfer (batch, any amount)
 * RTGS — Real Time Gross Settlement (real-time, min ₹2 lakh)
 */
public enum TransferType {
    IMPS,
    NEFT,
    RTGS
}
