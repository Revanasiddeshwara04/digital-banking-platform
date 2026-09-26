package com.revana.bank.transaction.controller;

import com.revana.bank.transaction.dto.TransactionRequest;
import com.revana.bank.transaction.entity.Transaction;
import com.revana.bank.transaction.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(
            TransactionService service) {

        this.service = service;
    }

    @PostMapping
    public Transaction createTransaction(
            @RequestBody TransactionRequest request) {

        return service.createTransaction(request);
    }

    @GetMapping
    public List<Transaction> getAllTransactions() {

        return service.getAllTransactions();
    }

    @GetMapping("/{id}")
    public Transaction getTransaction(
            @PathVariable Long id) {

        return service.getTransaction(id);
    }

    @GetMapping("/account/{accountId}")
    public List<Transaction> getByAccount(
            @PathVariable Long accountId) {

        return service.getByAccount(accountId);
    }
}