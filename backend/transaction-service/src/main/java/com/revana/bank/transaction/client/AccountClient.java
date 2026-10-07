package com.revana.bank.transaction.client;

// ════════════════════════════════════════════════════════════════════════════
// ── OLD CODE — no AccountClient existed in transaction-service before F3.
//              account-service exposes these endpoints (from AccountController):
//
//   GET  /api/accounts/search/account-number?accountNumber={n}  → Account entity
//   POST /api/accounts/{id}/deposit
//   POST /api/accounts/{id}/withdraw
//   POST /api/accounts/transfer
//
//   We target only the lookup endpoint here — balance debit/credit in
//   account-service is handled by account-service's own transfer logic.
//   TransferService calls this client to validate accounts before processing.
// ════════════════════════════════════════════════════════════════════════════

// ── NEW CODE ─────────────────────────────────────────────────────────────────

import com.revana.bank.transaction.client.dto.AccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for account-service.
 *
 * Used by TransferService to:
 *  1. Verify source and destination accounts exist.
 *  2. Check that both accounts are ACTIVE.
 *  3. Check that source account has sufficient balance.
 *
 * The service name "account-service" resolves via Eureka — no hardcoded URL.
 * A fallback / circuit-breaker can be added in a later feature via
 * @FeignClient(name="...", fallback=AccountClientFallback.class).
 */
@FeignClient(name = "account-service")
public interface AccountClient {

    /**
     * Fetches an account by its human-readable account number.
     * Maps to GET /api/accounts/search/account-number?accountNumber={n}
     *
     * @param accountNumber e.g. "ACC10001"
     * @return AccountResponse or null if not found (Feign returns null on 404)
     */
    @GetMapping("/api/accounts/search/account-number")
    AccountResponse getByAccountNumber(@RequestParam("accountNumber") String accountNumber);
}
