package com.revana.bank.transaction.service;

import com.revana.bank.transaction.entity.AuditLog;
import com.revana.bank.transaction.entity.TransferStatus;
import com.revana.bank.transaction.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Single-responsibility service that appends immutable audit records.
 *
 * Design notes:
 *  - Uses Propagation.REQUIRES_NEW so an audit log is ALWAYS written,
 *    even if the calling transaction rolls back. This guarantees that
 *    failed transfers (e.g. INSUFFICIENT_BALANCE) are still recorded.
 *  - Never throws — if audit writing itself fails, the error is logged
 *    and swallowed so the main transfer flow is not disrupted. Banking
 *    audits are best-effort; they must not cause fund movement failures.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    /**
     * Writes a single audit entry.
     *
     * @param action              event label, e.g. TRANSFER_INITIATED
     * @param userId              JWT subject of the caller
     * @param sourceAccount       debit-side account number
     * @param destinationAccount  credit-side account number
     * @param amount              transfer amount (null-safe)
     * @param status              transfer status at the time of this event
     * @param details             free-text or JSON context (error message, ref, etc.)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action,
                    String userId,
                    String sourceAccount,
                    String destinationAccount,
                    BigDecimal amount,
                    TransferStatus status,
                    String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .userId(userId != null ? userId : "SYSTEM")
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .amount(amount)
                    .status(status)
                    .details(details)
                    .build();

            auditLogRepository.save(entry);

        } catch (Exception ex) {
            // Audit failure must never propagate to the caller
            log.error("Failed to write audit log [action={}, userId={}, src={}, dst={}]: {}",
                    action, userId, sourceAccount, destinationAccount, ex.getMessage(), ex);
        }
    }
}
