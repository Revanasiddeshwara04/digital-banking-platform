package com.revana.bank.transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persistent record of every fund transfer processed by this service.
 *
 * Key design decisions:
 *  - transactionReference is auto-generated in @PrePersist using UUID
 *    to guarantee uniqueness without a sequence dependency.
 *  - status is stored as a VARCHAR enum so it is human-readable in the DB.
 *  - DB indexes are added on the columns most frequently used in WHERE clauses
 *    (sourceAccount, destinationAccount, transactionDate, status) to keep
 *    history and daily-limit queries fast at scale.
 *  - performedBy records the userId of the authenticated caller so every
 *    record is audit-traceable without joining audit_logs.
 */
@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_txn_source_account",      columnList = "sourceAccount"),
        @Index(name = "idx_txn_destination_account", columnList = "destinationAccount"),
        @Index(name = "idx_txn_transaction_date",    columnList = "transactionDate"),
        @Index(name = "idx_txn_status",              columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique human-readable reference returned to the caller, e.g. TXN4A3B2C1D. */
    @Column(unique = true, nullable = false, length = 30)
    private String transactionReference;

    /** Internal FK to the source account row (used by legacy lookups). */
    private Long fromAccountId;

    /** Internal FK to the destination account row (used by legacy lookups). */
    private Long toAccountId;

    /** Human-readable source account number, e.g. ACC10001. */
    @Column(nullable = false, length = 20)
    private String sourceAccount;

    /** Human-readable destination account number, e.g. ACC20001. */
    @Column(nullable = false, length = 20)
    private String destinationAccount;

    /** Payment rail: IMPS, NEFT, or RTGS. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransferType transferType;

    /** Broad category, e.g. TRANSFER, DEPOSIT, WITHDRAW — kept for legacy compatibility. */
    @Column(length = 30)
    private String transactionType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Optional free-text note provided by the customer, e.g. "Rent Payment". */
    @Column(length = 255)
    private String description;

    /** Current lifecycle state of this transfer. Defaults to PENDING on first insert. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransferStatus status;

    /** UserId of the authenticated user who initiated this transfer. */
    @Column(length = 60)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    /**
     * Runs before the first INSERT.
     * Generates a collision-resistant transaction reference and seeds defaults.
     */
    @PrePersist
    public void prePersist() {
        if (transactionReference == null) {
            // "TXN" + first 12 hex chars of a random UUID → e.g. TXN4A3B2C1D5E6F
            transactionReference = "TXN" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase();
        }
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (status == null) {
            status = TransferStatus.PENDING;
        }
    }
}