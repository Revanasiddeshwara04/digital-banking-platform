package com.revana.bank.kyc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(
        name = "customer-service",
contextId = "customerFeignClient"
)
public interface CustomerFeignClient {

    @PutMapping(
            "/api/customers/{customerId}/activate"
    )
    void activateCustomer(
            @PathVariable("customerId")
            String customerId);
}