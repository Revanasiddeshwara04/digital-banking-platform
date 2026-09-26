package com.revana.bank.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "auth-service")
public interface UserClient {

    @GetMapping(
            "/api/users/email/{accountId}")
    String getEmail(
            @PathVariable Long accountId);
}