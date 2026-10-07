package com.revana.bank.transaction.service;

import com.revana.bank.transaction.dto.TransactionRequest;
import com.revana.bank.transaction.entity.Transaction;
import com.revana.bank.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Legacy transaction service — retained for backward compatibility.
 *
 * The account-service calls this via Feign (POST /api/transactions) to
 * record deposit, withdrawal, and legacy-transfer events. Those callers
 * continue to work unchanged.
 *
 * New fund-transfer flows use {@link TransferService} directly.
 */
@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final TransferService transferService;

    public TransactionService(TransactionRepository repository, TransferService transferService) {
        this.repository = repository;
        this.transferService = transferService;
    }

    // ── Existing methods — untouched ─────────────────────────────────────

    public Transaction createTransaction(TransactionRequest request) {

        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .status(TransferStatus.valueOf(
                        request.getStatus() != null
                                ? request.getStatus().toUpperCase()
                                : "PENDING"))
                .transactionDate(LocalDateTime.now())
                .build();

        return repository.save(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return repository.findAll();
    }

    public Transaction getTransaction(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Transaction> getByAccount(Long accountId) {
        return repository.findByFromAccountIdOrToAccountId(accountId, accountId);
    }
    
    /**
     * New method to demonstrate integration with TransferService.
     * Converts legacy transaction to transfer format for reporting.
     */
    public String getTransferReferenceForTransaction(Long transactionId) {
        Transaction transaction = getTransaction(transactionId);
        if (transaction == null) {
            return null;
        }
        
        // In a real integration, we might look up the corresponding transfer
        // or convert transaction data to transfer format
        return "TXN-" + transaction.getId();
    }
}