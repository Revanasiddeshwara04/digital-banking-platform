package com.revana.bank.transaction.dto;

import com.revana.bank.transaction.entity.TransferStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Outbound payload for:
 *   POST /api/transfers          — transfer result
 *   GET  /api/transfers/{ref}    — single transfer lookup
 *
 * Always includes the transactionReference so the caller can
 * use it for subsequent status checks or history queries.
 */
@Data
@Builder
public class TransferResponse {

    /** Unique reference for this transfer, e.g. TXN4A3B2C1D5E6F. */
    private String transactionReference;

    private String sourceAccount;

    private String destinationAccount;

    private BigDecimal amount;

    /** Final status at the time this response was produced. */
    private TransferStatus status;

    /** Human-readable summary, e.g. "Transfer completed successfully". */
    private String message;

    /** Timestamp at which the transfer record was created. */
    private LocalDateTime timestamp;
}
