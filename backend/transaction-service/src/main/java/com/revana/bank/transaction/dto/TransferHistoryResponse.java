package com.revana.bank.transaction.dto;

import com.revana.bank.transaction.entity.TransferStatus;
import com.revana.bank.transaction.entity.TransferType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single item in a paginated transfer history response.
 *
 * Used by GET /api/transfers/history.
 * The caller receives a Page<TransferHistoryResponse> so they can
 * navigate large result sets without fetching all records at once.
 */
@Data
@Builder
public class TransferHistoryResponse {

    private String transactionReference;

    private String sourceAccount;

    private String destinationAccount;

    private BigDecimal amount;

    /** Payment rail used for this transfer. */
    private TransferType transferType;

    /** Optional customer-provided memo. */
    private String description;

    private TransferStatus status;

    private LocalDateTime createdAt;
}
