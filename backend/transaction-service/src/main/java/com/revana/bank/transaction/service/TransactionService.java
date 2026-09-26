package com.revana.bank.transaction.service;

import com.revana.bank.transaction.dto.TransactionRequest;
import com.revana.bank.transaction.entity.Transaction;
import com.revana.bank.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(
            TransactionRepository repository) {

        this.repository = repository;
    }

    public Transaction createTransaction(
            TransactionRequest request) {

        Transaction transaction =
                Transaction.builder()
                        .fromAccountId(
                                request.getFromAccountId())
                        .toAccountId(
                                request.getToAccountId())
                        .transactionType(
                                request.getTransactionType())
                        .amount(
                                request.getAmount())
                        .status(
                                request.getStatus())
                        .transactionDate(
                                LocalDateTime.now())
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

        return repository
                .findByFromAccountIdOrToAccountId(
                        accountId,
                        accountId);
    }
}