package com.revana.bank.transaction.dto;

import com.revana.bank.transaction.entity.TransferStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka event published to the 'bank-transfer-topic' after every
 * completed (SUCCESS or FAILED) fund transfer.
 *
 * Consumers:
 *   - notification-service  → sends customer notification
 *   - audit-service         → persists an immutable audit record
 *
 * The payload is intentionally flat (no nested objects) so it serialises
 * cleanly to JSON without custom Jackson configuration.
 */
@Data
@Builder
public class FundTransferCompletedEvent {

    /**
     * The unique transaction reference, e.g. TXN4A3B2C1D5E6F.
     * Matches the transactionReference field in the Transfer entity.
     */
    private String transactionId;

    private String sourceAccount;

    private String destinationAccount;

    private BigDecimal amount;

    /** Timestamp at which the transfer was finalised. */
    private LocalDateTime timestamp;

    /** Final transfer status — SUCCESS or FAILED. */
    private TransferStatus status;
}
