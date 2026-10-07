package com.revana.bank.transaction.client.dto;

// ════════════════════════════════════════════════════════════════════════════
// ── OLD CODE — no AccountResponse existed in transaction-service before F3.
//              account-service Account entity (source of truth) has:
//
//   private Long id;
//   private Long userId;
//   private String customerId;
//   private String accountNumber;
//   private String customerName;
//   private String accountType;
//   private BigDecimal balance;
//   private String status;          ← "ACTIVE" / "CLOSED"
//   private LocalDateTime createdAt;
//   private LocalDateTime closedAt;
//
//   We project only the fields TransferService needs for validation.
// ════════════════════════════════════════════════════════════════════════════

// ── NEW CODE ─────────────────────────────────────────────────────────────────

import lombok.Data;

import java.math.BigDecimal;

/**
 * Projection of the account-service Account entity.
 *
 * Only fields required for fund-transfer validation are mapped here.
 * Extra fields returned by account-service are silently ignored by Jackson
 * (no deserialization error), which keeps this DTO resilient to
 * account-service schema additions.
 */
@Data
public class AccountResponse {

    /** Internal account row ID. */
    private Long id;

    /** Human-readable account number, e.g. ACC10001. */
    private String accountNumber;

    /** Registered customer name. */
    private String customerName;

    /** Current balance — used for the INSUFFICIENT_BALANCE check. */
    private BigDecimal balance;

    /**
     * Account lifecycle state.
     * Expected values: "ACTIVE" or "CLOSED".
     * TransferService rejects any non-"ACTIVE" value.
     */
    private String status;
}
