package com.revana.bank.kyc.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "customer-service",
        contextId = "customerClient")
public interface CustomerClient {

    @PatchMapping(
            "/api/customers/{customerId}/activate")
    void activateCustomer(
            @PathVariable String customerId);
}