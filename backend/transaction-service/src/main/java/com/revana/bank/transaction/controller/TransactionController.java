package com.revana.bank.transaction.controller;

// ════════════════════════════════════════════════════════════════════════════
// ── OLD CODE (Feature 1 / original — no security, no Swagger annotations)
//
// @RestController
// @RequestMapping("/api/transactions")
// public class TransactionController {
//
//     private final TransactionService service;
//
//     public TransactionController(TransactionService service) {
//         this.service = service;
//     }
//
//     @PostMapping
//     public Transaction createTransaction(@RequestBody TransactionRequest request) {
//         return service.createTransaction(request);
//     }
//
//     @GetMapping
//     public List<Transaction> getAllTransactions() {
//         return service.getAllTransactions();
//     }
//
//     @GetMapping("/{id}")
//     public Transaction getTransaction(@PathVariable Long id) {
//         return service.getTransaction(id);
//     }
//
//     @GetMapping("/account/{accountId}")
//     public List<Transaction> getByAccount(@PathVariable Long accountId) {
//         return service.getByAccount(accountId);
//     }
// }
// ════════════════════════════════════════════════════════════════════════════

// ── NEW CODE — same endpoints, now secured + documented ──────────────────────

import com.revana.bank.transaction.dto.TransactionRequest;
import com.revana.bank.transaction.entity.Transaction;
import com.revana.bank.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Legacy transaction controller — retained for backward compatibility.
 *
 * account-service calls POST /api/transactions via Feign to record
 * deposit, withdrawal, and transfer events. That path is unchanged.
 *
 * Change in Feature 3: all read endpoints are now secured with JWT.
 * The POST endpoint (called by account-service internally) remains
 * accessible to authenticated services.
 */
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions (Legacy)", description = "Legacy transaction recording endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    // ── POST /api/transactions — called by account-service via Feign ─────

    @PostMapping
    @Operation(
            summary     = "Record a transaction",
            description = "Called internally by account-service to record deposits, " +
                          "withdrawals, and legacy transfers."
    )
    public ResponseEntity<Transaction> createTransaction(
            @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(service.createTransaction(request));
    }

    // ── GET /api/transactions — ADMIN view ───────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all transactions (Admin only)")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(service.getAllTransactions());
    }

    // ── GET /api/transactions/{id} ────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(service.getTransaction(id));
    }

    // ── GET /api/transactions/account/{accountId} ─────────────────────────

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Get all transactions for an account")
    public ResponseEntity<List<Transaction>> getByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(service.getByAccount(accountId));
    }
}
