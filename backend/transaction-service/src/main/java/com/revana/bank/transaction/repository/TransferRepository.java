package com.revana.bank.transaction.repository;

import com.revana.bank.transaction.entity.Transfer;
import com.revana.bank.transaction.entity.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link Transfer}.
 *
 * Query groups:
 *  1. Reference    — single record lookup by transaction reference
 *  2. History      — paginated with optional status / date-range filters
 *                    (customer view: by sourceAccount)
 *  3. Admin view   — paginated across all accounts
 *  4. Daily limit  — aggregate for the ₹5,00,000 per-day business rule
 */
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    // ── 1. Reference lookup ───────────────────────────────────────────────

    /**
     * Fetch a single transfer by its unique transaction reference.
     * Used by GET /api/transfers/{transactionReference}.
     */
    Optional<Transfer> findByTransactionReference(String transactionReference);

    // ── 2. Customer history (paginated) ──────────────────────────────────

    /**
     * All transfers initiated from a source account — no additional filters.
     */
    Page<Transfer> findBySourceAccount(
            String sourceAccount,
            Pageable pageable);

    /**
     * All transfers for a source account filtered by status only.
     */
    Page<Transfer> findBySourceAccountAndStatus(
            String sourceAccount,
            TransferStatus status,
            Pageable pageable);

    /**
     * All transfers for a source account within a date range.
     */
    Page<Transfer> findBySourceAccountAndCreatedAtBetween(
            String sourceAccount,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    /**
     * All transfers for a source account filtered by status AND date range.
     */
    Page<Transfer> findBySourceAccountAndStatusAndCreatedAtBetween(
            String sourceAccount,
            TransferStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    // ── 3. Non-paginated convenience queries (legacy / internal use) ──────

    List<Transfer> findByDestinationAccount(String destinationAccount);

    List<Transfer> findByStatus(TransferStatus status);

    // ── 4. Daily-limit aggregate ──────────────────────────────────────────

    /**
     * Returns the total amount successfully transferred OUT of sourceAccount
     * since the given startOfDay timestamp.
     *
     * Used by TransferService to enforce the ₹5,00,000 daily transfer limit.
     * COALESCE guarantees 0 is returned when no qualifying rows exist.
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM   Transfer t
            WHERE  t.sourceAccount = :sourceAccount
              AND  t.status        = 'SUCCESS'
              AND  t.createdAt    >= :startOfDay
            """)
    BigDecimal sumSuccessfulTransfersAfter(
            @Param("sourceAccount") String sourceAccount,
            @Param("startOfDay")    LocalDateTime startOfDay);
}
