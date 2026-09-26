package com.revana.bank.account.service;

import com.revana.bank.account.client.TransactionClient;
import com.revana.bank.account.dto.TransactionRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class TransactionClientService {

    private final TransactionClient transactionClient;

    public TransactionClientService(
            TransactionClient transactionClient) {

        this.transactionClient = transactionClient;
    }

    @Retry(
            name = "transactionService")
    @CircuitBreaker(
            name = "transactionService",
            fallbackMethod = "transactionFallback")
    public void createTransaction(
            TransactionRequest request) {

        transactionClient.createTransaction(request);
    }

    public void transactionFallback(
            TransactionRequest request,
            Throwable ex) {

        System.out.println(
                "Transaction Service Down. Transaction skipped.");

        System.out.println(
                "Reason = " + ex.getClass().getName());

        System.out.println(
                "Message = " + ex.getMessage());
    }
}