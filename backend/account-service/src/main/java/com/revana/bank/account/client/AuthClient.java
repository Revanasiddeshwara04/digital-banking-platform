package com.revana.bank.account.client;

import com.revana.bank.account.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "auth-service"
)
public interface AuthClient {

    @GetMapping("/api/users/me")
    UserResponse getCurrentUser(
            @RequestHeader("Authorization")
            String token);
}