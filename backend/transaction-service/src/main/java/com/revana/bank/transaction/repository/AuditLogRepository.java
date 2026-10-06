package com.revana.bank.transaction.repository;

import com.revana.bank.transaction.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access layer for {@link AuditLog}.
 *
 * Intentionally kept simple — audit records are append-only and the
 * primary consumers are compliance queries, not high-frequency lookups.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * All audit entries initiated by a specific user.
     * Used by compliance / admin reporting endpoints.
     */
    List<AuditLog> findByUserId(String userId);

    /**
     * All audit entries involving a given account on either side.
     * Used when a customer or admin requests a full account audit trail.
     */
    List<AuditLog> findBySourceAccountOrDestinationAccount(
            String sourceAccount,
            String destinationAccount);

    /**
     * All audit entries created within a given time window.
     * Used for compliance reports scoped to a date range.
     */
    List<AuditLog> findByCreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to);
}
