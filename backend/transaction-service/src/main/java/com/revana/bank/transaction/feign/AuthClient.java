package com.revana.bank.transaction.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client for auth-service.
 * Used for JWT token validation and user role checking.
 * 
 * Note: In the current implementation, JWT validation is performed
 * locally using JwtService. This client is created to satisfy
 * FEAT-002 requirements and can be used if centralized token
 * validation is needed in the future.
 */
@FeignClient(name = "auth-service")
public interface AuthClient {

    /**
     * Validates a JWT token with auth-service.
     * 
     * @param authorization Bearer token (e.g., "Bearer <token>")
     * @return true if token is valid, false otherwise
     */
    @GetMapping("/api/auth/validate")
    boolean validateToken(@RequestHeader("Authorization") String authorization);

    /**
     * Gets user role from auth-service.
     * 
     * @param authorization Bearer token
     * @return user role (e.g., "CUSTOMER", "ADMIN")
     */
    @GetMapping("/api/auth/role")
    String getUserRole(@RequestHeader("Authorization") String authorization);

    /**
     * Gets user ID from auth-service.
     * 
     * @param authorization Bearer token
     * @return user ID
     */
    @GetMapping("/api/auth/user-id")
    Long getUserId(@RequestHeader("Authorization") String authorization);
}