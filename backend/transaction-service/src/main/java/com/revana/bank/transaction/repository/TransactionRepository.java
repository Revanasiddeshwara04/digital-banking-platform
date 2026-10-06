package com.revana.bank.transaction.repository;

import com.revana.bank.transaction.entity.Transaction;

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
 * Data access layer for {@link Transaction}.
 *
 * Query methods are organised into four groups:
 *  1. Legacy lookup  — by internal account IDs (kept for backward-compat)
 *  2. Reference      — single record by transaction reference number
 *  3. History        — paginated, with optional status / date-range filters
 *  4. Daily limit    — aggregate used by the business-rule guard in TransferService
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ── 1. Legacy lookup (preserved from original implementation) ─────────

    List<Transaction> findByFromAccountIdOrToAccountId(
            Long fromAccountId,
            Long toAccountId);

    // ── 2. Reference lookup ───────────────────────────────────────────────

    /**
     * Fetch a single transaction by its unique human-readable reference.
     * Used by GET /api/transfers/{transactionReference}.
     */
    Optional<Transaction> findByTransactionReference(String transactionReference);

    // ── 3. History queries (paginated) ────────────────────────────────────

    /**
     * All transactions for a source account, newest first.
     * Used when neither status nor date range is supplied.
     */
    Page<Transaction> findBySourceAccount(
            String sourceAccount,
            Pageable pageable);

    /**
     * All transactions for a source account filtered by status only.
     */
    Page<Transaction> findBySourceAccountAndStatus(
            String sourceAccount,
            String status,
            Pageable pageable);

    /**
     * All transactions for a source account within a date range.
     */
    Page<Transaction> findBySourceAccountAndTransactionDateBetween(
            String sourceAccount,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    /**
     * All transactions for a source account filtered by both status AND date range.
     */
    Page<Transaction> findBySourceAccountAndStatusAndTransactionDateBetween(
            String sourceAccount,
            String status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    // ── 4. Daily-limit aggregate ──────────────────────────────────────────

    /**
     * Returns the total amount successfully transferred OUT of sourceAccount
     * since the given startOfDay timestamp.
     *
     * Used by TransferService to enforce the ₹5,00,000 daily limit.
     * COALESCE ensures 0 is returned when there are no qualifying rows.
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM   Transaction t
            WHERE  t.sourceAccount  = :sourceAccount
              AND  t.status         = 'SUCCESS'
              AND  t.transactionDate >= :startOfDay
            """)
    BigDecimal sumSuccessfulTransfersAfter(
            @Param("sourceAccount") String sourceAccount,
            @Param("startOfDay")    LocalDateTime startOfDay);
}
