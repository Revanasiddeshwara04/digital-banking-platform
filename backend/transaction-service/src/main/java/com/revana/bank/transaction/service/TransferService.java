package com.revana.bank.transaction.service;

import com.revana.bank.transaction.client.AccountClient;
import com.revana.bank.transaction.client.dto.AccountResponse;
import com.revana.bank.transaction.dto.FundTransferCompletedEvent;
import com.revana.bank.transaction.dto.TransferHistoryResponse;
import com.revana.bank.transaction.dto.TransferRequest;
import com.revana.bank.transaction.dto.TransferResponse;
import com.revana.bank.transaction.entity.Transfer;
import com.revana.bank.transaction.entity.TransferStatus;
import com.revana.bank.transaction.exception.ErrorCode;
import com.revana.bank.transaction.exception.TransferException;
import com.revana.bank.transaction.kafka.TransferKafkaProducer;
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
 * CHANGE LOG (Feature 3):
 *   - AccountClient injected → steps 3 & 4 (account validation + balance check) are now live.
 *   - TransferKafkaProducer injected → step 11 (Kafka publish) is now live.
 *   - All TODO comments replaced with real implementations.
 *   - Old TODO blocks preserved as comments below each section for comparison.
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    /** ₹5,00,000 daily transfer cap per source account. */
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("500000");

    private final TransferRepository    transferRepository;
    private final AuditService          auditService;

    // ── NEW (Feature 3): wired dependencies ──────────────────────────────
    private final AccountClient         accountClient;
    private final TransferKafkaProducer kafkaProducer;

    // ════════════════════════════════════════════════════════════════════════
    // ── OLD CODE (Feature 2 constructor dependencies — no AccountClient/Kafka)
    //
    //  private final TransferRepository transferRepository;
    //  private final AuditService       auditService;
    //  // TODO (Feature 3): inject AccountClient and KafkaProducerService
    // ════════════════════════════════════════════════════════════════════════

    // =========================================================================
    // Initiate transfer — POST /api/transfers
    // =========================================================================

    /**
     * Validates and processes a fund transfer request.
     *
     * Steps:
     *  1.  Self-transfer guard
     *  2.  Amount guard
     *  3.  Source account: exists + ACTIVE          ← NOW LIVE (was TODO)
     *  4.  Destination account: exists + ACTIVE     ← NOW LIVE (was TODO)
     *  5.  Sufficient balance check                 ← NOW LIVE (was TODO)
     *  6.  Daily limit check
     *  7.  Persist Transfer in PENDING state
     *  8.  Audit: TRANSFER_INITIATED
     *  9.  Mark Transfer SUCCESS
     *  10. Audit: TRANSFER_SUCCESS
     *  11. Publish Kafka event                      ← NOW LIVE (was TODO)
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

        // ── 3. Source account validation ──────────────────────────────────
        // ════════════════════════════════════════════════════════════════
        // OLD CODE (Feature 2 — was a TODO comment):
        //
        // // AccountResponse source = accountClient.getByAccountNumber(...);
        // // if (source == null) throw new TransferException(ACCOUNT_NOT_FOUND,...);
        // // if (!"ACTIVE".equals(source.getStatus())) throw new TransferException(ACCOUNT_INACTIVE,...);
        // ════════════════════════════════════════════════════════════════
        AccountResponse source = accountClient.getByAccountNumber(request.getSourceAccount());
        if (source == null) {
            auditService.log("TRANSFER_REJECTED", userId,
                    request.getSourceAccount(), request.getDestinationAccount(),
                    request.getAmount(), TransferStatus.FAILED,
                    "Source account not found: " + request.getSourceAccount());
            throw new TransferException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "Source account not found: " + request.getSourceAccount());
        }
        if (!"ACTIVE".equals(source.getStatus())) {
            auditService.log("TRANSFER_REJECTED", userId,
                    request.getSourceAccount(), request.getDestinationAccount(),
                    request.getAmount(), TransferStatus.FAILED,
                    "Source account inactive: " + request.getSourceAccount());
            throw new TransferException(ErrorCode.ACCOUNT_INACTIVE,
                    "Source account is not active: " + request.getSourceAccount());
        }

        // ── 4. Destination account validation ─────────────────────────────
        // ════════════════════════════════════════════════════════════════
        // OLD CODE (Feature 2 — was a TODO comment):
        //
        // // AccountResponse dest = accountClient.getByAccountNumber(...);
        // // if (dest == null) throw new TransferException(ACCOUNT_NOT_FOUND,...);
        // // if (!"ACTIVE".equals(dest.getStatus())) throw new TransferException(ACCOUNT_INACTIVE,...);
        // ════════════════════════════════════════════════════════════════
        AccountResponse dest = accountClient.getByAccountNumber(request.getDestinationAccount());
        if (dest == null) {
            auditService.log("TRANSFER_REJECTED", userId,
                    request.getSourceAccount(), request.getDestinationAccount(),
                    request.getAmount(), TransferStatus.FAILED,
                    "Destination account not found: " + request.getDestinationAccount());
            throw new TransferException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "Destination account not found: " + request.getDestinationAccount());
        }
        if (!"ACTIVE".equals(dest.getStatus())) {
            auditService.log("TRANSFER_REJECTED", userId,
                    request.getSourceAccount(), request.getDestinationAccount(),
                    request.getAmount(), TransferStatus.FAILED,
                    "Destination account inactive: " + request.getDestinationAccount());
            throw new TransferException(ErrorCode.ACCOUNT_INACTIVE,
                    "Destination account is not active: " + request.getDestinationAccount());
        }

        // ── 5. Sufficient balance check ───────────────────────────────────
        // ════════════════════════════════════════════════════════════════
        // OLD CODE (Feature 2 — was a TODO comment):
        //
        // // if (source.getBalance().compareTo(request.getAmount()) < 0) {
        // //     auditService.log("TRANSFER_REJECTED", userId, ...);
        // //     throw new TransferException(ErrorCode.INSUFFICIENT_BALANCE);
        // // }
        // ════════════════════════════════════════════════════════════════
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            auditService.log("TRANSFER_REJECTED", userId,
                    request.getSourceAccount(), request.getDestinationAccount(),
                    request.getAmount(), TransferStatus.FAILED,
                    "Insufficient balance. Available: " + source.getBalance()
                            + " | Requested: " + request.getAmount());
            throw new TransferException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // ── 6. Daily limit check ──────────────────────────────────────────
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        BigDecimal todayTotal = transferRepository
                .sumSuccessfulTransfersAfter(request.getSourceAccount(), startOfDay);
        if (todayTotal == null) todayTotal = BigDecimal.ZERO;

        if (todayTotal.add(request.getAmount()).compareTo(DAILY_LIMIT) > 0) {
            auditService.log("TRANSFER_REJECTED", userId,
                    request.getSourceAccount(), request.getDestinationAccount(),
                    request.getAmount(), TransferStatus.FAILED,
                    ErrorCode.DAILY_LIMIT_EXCEEDED.name()
                            + " | todayTotal=" + todayTotal);
            throw new TransferException(ErrorCode.DAILY_LIMIT_EXCEEDED);
        }

        // ── 7. Persist in PENDING state ───────────────────────────────────
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

        // ── 8. Audit: INITIATED ───────────────────────────────────────────
        auditService.log("TRANSFER_INITIATED", userId,
                request.getSourceAccount(), request.getDestinationAccount(),
                request.getAmount(), TransferStatus.PENDING,
                transfer.getTransactionReference());

        // ── 9. Mark SUCCESS ───────────────────────────────────────────────
        // Note: actual debit/credit on the account-service side is owned by
        // account-service's own transfer endpoint. This service records the
        // event and status; account-service handles balance mutation.
        transfer.setStatus(TransferStatus.SUCCESS);
        transfer = transferRepository.save(transfer);
        log.info("Transfer completed: ref={}", transfer.getTransactionReference());

        // ── 10. Audit: SUCCESS ────────────────────────────────────────────
        auditService.log("TRANSFER_SUCCESS", userId,
                request.getSourceAccount(), request.getDestinationAccount(),
                request.getAmount(), TransferStatus.SUCCESS,
                transfer.getTransactionReference());

        // ── 11. Publish Kafka event ───────────────────────────────────────
        // ════════════════════════════════════════════════════════════════
        // OLD CODE (Feature 2 — was a TODO comment):
        //
        // // FundTransferCompletedEvent event = FundTransferCompletedEvent.builder()
        // //         .transactionId(transfer.getTransactionReference())
        // //         ...
        // //         .build();
        // // kafkaProducer.publishTransferCompleted(event);
        // ════════════════════════════════════════════════════════════════
        FundTransferCompletedEvent event = FundTransferCompletedEvent.builder()
                .transactionId(transfer.getTransactionReference())
                .sourceAccount(transfer.getSourceAccount())
                .destinationAccount(transfer.getDestinationAccount())
                .amount(transfer.getAmount())
                .timestamp(transfer.getCreatedAt())
                .status(TransferStatus.SUCCESS)
                .build();
        kafkaProducer.publishTransferCompleted(event);

        return mapToTransferResponse(transfer, "Transfer completed successfully");
    }

    // =========================================================================
    // Lookup by reference — GET /api/transfers/{transactionReference}
    // =========================================================================

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

    @Transactional(readOnly = true)
    public Page<TransferHistoryResponse> getHistory(String sourceAccount,
                                                    TransferStatus status,
                                                    LocalDateTime from,
                                                    LocalDateTime to,
                                                    Pageable pageable) {
        Page<Transfer> page;

        boolean hasStatus    = status != null;
        boolean hasDateRange = from != null && to != null;

        if (hasStatus && hasDateRange) {
            page = transferRepository.findBySourceAccountAndStatusAndCreatedAtBetween(
                    sourceAccount, status, from, to, pageable);
        } else if (hasStatus) {
            page = transferRepository.findBySourceAccountAndStatus(
                    sourceAccount, status, pageable);
        } else if (hasDateRange) {
            page = transferRepository.findBySourceAccountAndCreatedAtBetween(
                    sourceAccount, from, to, pageable);
        } else {
            page = transferRepository.findBySourceAccount(sourceAccount, pageable);
        }

        return page.map(this::mapToHistoryResponse);
    }

    @Transactional(readOnly = true)
    public List<Transfer> getTransfersByAccount(String sourceAccount) {
        return transferRepository.findByDestinationAccount(sourceAccount);
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
