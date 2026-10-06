package com.revana.bank.transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dedicated entity for the 'transfers' table.
 *
 * This is the primary record created when a customer initiates a fund transfer
 * via POST /api/transfers. It maps directly to the transfers table defined in
 * the business requirements.
 *
 * Design decisions:
 *  - transactionReference uses UUID-based generation for collision safety.
 *  - DB indexes on sourceAccount, destinationAccount, status, and createdAt
 *    keep history and filter queries performant.
 *  - performedBy stores the JWT subject (userId) for traceability.
 *  - Both TransferType and TransferStatus are persisted as readable VARCHAR strings.
 */
@Entity
@Table(
    name = "transfers",
    indexes = {
        @Index(name = "idx_trf_source_account",      columnList = "sourceAccount"),
        @Index(name = "idx_trf_destination_account", columnList = "destinationAccount"),
        @Index(name = "idx_trf_status",              columnList = "status"),
        @Index(name = "idx_trf_created_at",          columnList = "createdAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique human-readable reference returned to the caller, e.g. TXN4A3B2C1D5E6F. */
    @Column(unique = true, nullable = false, length = 30)
    private String transactionReference;

    @Column(nullable = false, length = 20)
    private String sourceAccount;

    @Column(nullable = false, length = 20)
    private String destinationAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Payment rail: IMPS, NEFT, or RTGS. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransferType transferType;

    /** Optional customer-provided memo, e.g. "Rent Payment". */
    @Column(length = 255)
    private String description;

    /** Current lifecycle state. Defaults to PENDING on first insert. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransferStatus status;

    /** UserId of the authenticated caller (JWT sub claim). */
    @Column(length = 60)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Seeds defaults before the first INSERT.
     * Generates a UUID-based, collision-resistant reference (e.g. TXN4A3B2C1D5E6F).
     */
    @PrePersist
    public void prePersist() {
        if (transactionReference == null) {
            transactionReference = "TXN" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TransferStatus.PENDING;
        }
    }
}
