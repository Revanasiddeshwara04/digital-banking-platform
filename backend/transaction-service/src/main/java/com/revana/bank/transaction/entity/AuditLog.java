package com.revana.bank.transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable audit record written for every transfer attempt — success or failure.
 *
 * Design principles:
 *  - One row per significant event (TRANSFER_INITIATED, TRANSFER_SUCCESS,
 *    TRANSFER_FAILED, TRANSFER_REVERSED).
 *  - Records are never updated; new rows are appended for each state change.
 *  - The 'details' column holds a free-text or JSON payload for forensic use,
 *    so no schema migration is needed when the payload shape evolves.
 *  - Indexes on action and createdAt support compliance queries
 *    ("show me all failures in the last 30 days").
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_action",     columnList = "action"),
        @Index(name = "idx_audit_user_id",    columnList = "userId"),
        @Index(name = "idx_audit_created_at", columnList = "createdAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Event label.
     * Examples: TRANSFER_INITIATED, TRANSFER_SUCCESS, TRANSFER_FAILED,
     *           TRANSFER_REVERSED, DAILY_LIMIT_EXCEEDED, INVALID_AMOUNT.
     */
    @Column(nullable = false, length = 60)
    private String action;

    /** Authenticated userId (sub claim from JWT) who triggered the action. */
    @Column(nullable = false, length = 60)
    private String userId;

    @Column(length = 20)
    private String sourceAccount;

    @Column(length = 20)
    private String destinationAccount;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    /** Final or intermediate transfer status at the time of this audit entry. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TransferStatus status;

    /**
     * Free-text or JSON payload with additional context.
     * Examples: error message, transactionReference, failure reason.
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
