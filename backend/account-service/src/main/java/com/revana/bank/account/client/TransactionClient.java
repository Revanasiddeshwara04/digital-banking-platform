package com.revana.bank.account.client;

import com.revana.bank.account.dto.TransactionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "transaction-service",
        url = "http://localhost:8083"
)
public interface TransactionClient {

    @PostMapping("/api/transactions")
    void createTransaction(
            TransactionRequest request);
}