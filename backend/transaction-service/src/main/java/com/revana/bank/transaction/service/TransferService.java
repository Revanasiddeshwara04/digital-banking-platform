package com.revana.bank.transaction.service;

import com.revana.bank.transaction.dto.FundTransferCompletedEvent;
import com.revana.bank.transaction.dto.TransferHistoryResponse;
import com.revana.bank.transaction.dto.TransferRequest;
import com.revana.bank.transaction.dto.TransferResponse;
import com.revana.bank.transaction.entity.Transfer;
import com.revana.bank.transaction.entity.TransferStatus;
import com.revana.bank.transaction.exception.ErrorCode;
import com.revana.bank.transaction.exception.TransferException;
import com.revana.bank.transaction.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core business logic for the Fund Transfer feature.
 *
 * Responsibility boundaries:
 *  - This service owns all business-rule validation (amount > 0, no self-transfer,
 *    daily limit, account status, sufficient balance).
 *  - Account validation and balance debit/credit are delegated to account-service
 *    via AccountClient (Feign) — hooks marked TODO are wired in Feature 3.
 *  - Audit entries are always written via AuditService (REQUIRES_NEW tx),
 *    even when the transfer itself fails.
 *  - The Kafka event is published after the database commit to avoid publishing
 *    events for transactions that later roll back — wired in Feature 3.
 *
 * Daily transfer limit: ₹5,00,000 per source account per calendar day.
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    /** ₹5,00,000 daily transfer cap per source account. */
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("500000");

    private final TransferRepository transferRepository;
    private final AuditService auditService;

    // TODO (Feature 3): inject AccountClient and KafkaProducerService

    // =========================================================================
    // Initiate transfer — POST /api/transfers
    // =========================================================================

    /**
     * Validates and processes a fund transfer request.
     *
     * Steps:
     *  1.  Self-transfer guard
     *  2.  Amount guard
     *  3.  Account existence + ACTIVE status check  (AccountClient — Feature 3)
     *  4.  Sufficient balance check                  (AccountClient — Feature 3)
     *  5.  Daily limit check
     *  6.  Persist Transfer in PENDING state
     *  7.  Audit: TRANSFER_INITIATED
     *  8.  Debit source, credit destination          (AccountClient — Feature 3)
     *  9.  Mark Transfer SUCCESS
     *  10. Audit: TRANSFER_SUCCESS
     *  11. Publish Kafka event                        (KafkaProducer — Feature 3)
     *
     * @param request validated inbound DTO
     * @param userId  JWT subject of the authenticated caller
     * @return populated TransferResponse with transaction reference and status
     */
    @Transactional
    public TransferResponse initiateTransfer(TransferRequest request, String userId) {

        log.info("Initiating transfer: src={} dst={} amount={} user={}",
                request.getSourceAccount(), request.getDestinationAccount(),
                request.getAmount(), userId);

        // ── 1. Self-transfer guard ────────────────────────────────────────
        if (request.getSourceAccount().equalsIgnoreCase(request.getDestinationAccount())) {
            auditService.log("TRANSFER_REJECTED", userId,
                    request.getSourceAccount(), request.getDestinationAccount(),
                    request.getAmount(), TransferStatus.FAILED,
                    ErrorCode.SELF_TRANSFER_NOT_ALLOWED.name());
            throw new TransferException(ErrorCode.SELF_TRANSFER_NOT_ALLOWED);
        }

        // ── 2. Amount guard ───────────────────────────────────────────────
        if (request.getAmount() == null ||
                request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferException(ErrorCode.INVALID_TRANSFER_AMOUNT);
        }

        // ── 3 & 4. Account validation + balance check (Feature 3 TODO) ───
        //
        // AccountResponse source = accountClient.getByAccountNumber(request.getSourceAccount());
        // if (source == null)
        //     throw new TransferException(ErrorCode.ACCOUNT_NOT_FOUND,
        //             "Source account not found: " + request.getSourceAccount());
        // if (!"ACTIVE".equals(source.getStatus()))
        //     throw new TransferException(ErrorCode.ACCOUNT_INACTIVE,
        //             "Source account is not active: " + request.getSourceAccount());
        //
        // AccountResponse dest = accountClient.getByAccountNumber(request.getDestinationAccount());
        // if (dest == null)
        //     throw new TransferException(ErrorCode.ACCOUNT_NOT_FOUND,
        //             "Destination account not found: " + request.getDestinationAccount());
        // if (!"ACTIVE".equals(dest.getStatus()))
        //     throw new TransferException(ErrorCode.ACCOUNT_INACTIVE,
        //             "Destination account is not active: " + request.getDestinationAccount());
        //
        // if (source.getBalance().compareTo(request.getAmount()) < 0) {
        //     auditService.log("TRANSFER_REJECTED", userId, ...);
        //     throw new TransferException(ErrorCode.INSUFFICIENT_BALANCE);
        // }

        // ── 5. Daily limit check ──────────────────────────────────────────
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        BigDecimal todayTotal = transferRepository
                .sumSuccessfulTransfersAfter(request.getSourceAccount(), startOfDay);

        // sumSuccessfulTransfersAfter returns COALESCE(SUM, 0) so null-safe
        if (todayTotal == null) todayTotal = BigDecimal.ZERO;

        if (todayTotal.add(request.getAmount()).compareTo(DAILY_LIMIT) > 0) {
            auditService.log("TRANSFER_REJECTED", userId,
                    request.getSourceAccount(), request.getDestinationAccount(),
                    request.getAmount(), TransferStatus.FAILED,
                    ErrorCode.DAILY_LIMIT_EXCEEDED.name() +
                            " | todayTotal=" + todayTotal);
            throw new TransferException(ErrorCode.DAILY_LIMIT_EXCEEDED);
        }

        // ── 6. Persist in PENDING state ───────────────────────────────────
        Transfer transfer = Transfer.builder()
                .sourceAccount(request.getSourceAccount())
                .destinationAccount(request.getDestinationAccount())
                .amount(request.getAmount())
                .transferType(request.getTransferType())
                .description(request.getDescription())
                .status(TransferStatus.PENDING)
                .performedBy(userId)
                .build();

        transfer = transferRepository.save(transfer);
        log.info("Transfer saved as PENDING: ref={}", transfer.getTransactionReference());

        // ── 7. Audit: INITIATED ───────────────────────────────────────────
        auditService.log("TRANSFER_INITIATED", userId,
                request.getSourceAccount(), request.getDestinationAccount(),
                request.getAmount(), TransferStatus.PENDING,
                transfer.getTransactionReference());

        // ── 8. Debit / credit via AccountClient (Feature 3 TODO) ─────────
        //
        // try {
        //     accountClient.debit(request.getSourceAccount(), request.getAmount());
        //     accountClient.credit(request.getDestinationAccount(), request.getAmount());
        // } catch (Exception ex) {
        //     transfer.setStatus(TransferStatus.FAILED);
        //     transferRepository.save(transfer);
        //     auditService.log("TRANSFER_FAILED", userId, ...);
        //     throw new TransferException(ErrorCode.TRANSFER_PROCESSING_FAILED);
        // }

        // ── 9. Mark SUCCESS ───────────────────────────────────────────────
        transfer.setStatus(TransferStatus.SUCCESS);
        transfer = transferRepository.save(transfer);
        log.info("Transfer completed: ref={}", transfer.getTransactionReference());

        // ── 10. Audit: SUCCESS ────────────────────────────────────────────
        auditService.log("TRANSFER_SUCCESS", userId,
                request.getSourceAccount(), request.getDestinationAccount(),
                request.getAmount(), TransferStatus.SUCCESS,
                transfer.getTransactionReference());

        // ── 11. Publish Kafka event (Feature 3 TODO) ─────────────────────
        //
        // FundTransferCompletedEvent event = FundTransferCompletedEvent.builder()
        //         .transactionId(transfer.getTransactionReference())
        //         .sourceAccount(transfer.getSourceAccount())
        //         .destinationAccount(transfer.getDestinationAccount())
        //         .amount(transfer.getAmount())
        //         .timestamp(transfer.getCreatedAt())
        //         .status(TransferStatus.SUCCESS)
        //         .build();
        // kafkaProducer.publishTransferCompleted(event);

        return mapToTransferResponse(transfer, "Transfer completed successfully");
    }

    // =========================================================================
    // Lookup by reference — GET /api/transfers/{transactionReference}
    // =========================================================================

    /**
     * Returns the transfer record for the given transaction reference.
     *
     * @throws TransferException(TRANSACTION_NOT_FOUND) if no match
     */
    @Transactional(readOnly = true)
    public TransferResponse getByReference(String transactionReference) {
        Transfer transfer = transferRepository
                .findByTransactionReference(transactionReference)
                .orElseThrow(() -> new TransferException(
                        ErrorCode.TRANSACTION_NOT_FOUND,
                        "No transfer found for reference: " + transactionReference));

        return mapToTransferResponse(transfer, "Transfer retrieved successfully");
    }

    // =========================================================================
    // History — GET /api/transfers/history
    // =========================================================================

    /**
     * Returns a paginated history for the given source account.
     * All filter parameters are optional — supply null to omit a filter.
     *
     * @param sourceAccount account number to filter on (required)
     * @param status        optional status filter
     * @param from          optional start of date range (inclusive)
     * @param to            optional end of date range (inclusive)
     * @param pageable      pagination and sort
     */
    @Transactional(readOnly = true)
    public Page<TransferHistoryResponse> getHistory(String sourceAccount,
                                                    TransferStatus status,
                                                    LocalDateTime from,
                                                    LocalDateTime to,
                                                    Pageable pageable) {

        Page<Transfer> page;

        boolean hasStatus = status != null;
        boolean hasDateRange = from != null && to != null;

        if (hasStatus && hasDateRange) {
            page = transferRepository
                    .findBySourceAccountAndStatusAndCreatedAtBetween(
                            sourceAccount, status, from, to, pageable);

        } else if (hasStatus) {
            page = transferRepository
                    .findBySourceAccountAndStatus(sourceAccount, status, pageable);

        } else if (hasDateRange) {
            page = transferRepository
                    .findBySourceAccountAndCreatedAtBetween(
                            sourceAccount, from, to, pageable);

        } else {
            page = transferRepository
                    .findBySourceAccount(sourceAccount, pageable);
        }

        return page.map(this::mapToHistoryResponse);
    }

    /**
     * Returns all transfers for a given source account (non-paginated).
     * Kept for backward compatibility with legacy callers.
     */
    @Transactional(readOnly = true)
    public List<Transfer> getTransfersByAccount(String sourceAccount) {
        return transferRepository
                .findByDestinationAccount(sourceAccount); // kept; callers can use history endpoint
    }

    // =========================================================================
    // Private mapping helpers
    // =========================================================================

    private TransferResponse mapToTransferResponse(Transfer t, String message) {
        return TransferResponse.builder()
                .transactionReference(t.getTransactionReference())
                .sourceAccount(t.getSourceAccount())
                .destinationAccount(t.getDestinationAccount())
                .amount(t.getAmount())
                .status(t.getStatus())
                .message(message)
                .timestamp(t.getCreatedAt())
                .build();
    }

    private TransferHistoryResponse mapToHistoryResponse(Transfer t) {
        return TransferHistoryResponse.builder()
                .transactionReference(t.getTransactionReference())
                .sourceAccount(t.getSourceAccount())
                .destinationAccount(t.getDestinationAccount())
                .amount(t.getAmount())
                .transferType(t.getTransferType())
                .description(t.getDescription())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
