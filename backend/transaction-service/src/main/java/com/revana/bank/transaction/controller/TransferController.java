package com.revana.bank.transaction.controller;

// ════════════════════════════════════════════════════════════════════════════
// ── OLD CODE — no TransferController existed before Feature 3.
//              Original TransactionController (kept unchanged) handles legacy
//              POST /api/transactions endpoint only.
//              New /api/transfers endpoints live exclusively here.
// ════════════════════════════════════════════════════════════════════════════

// ── NEW CODE ─────────────────────────────────────────────────────────────────

import com.revana.bank.transaction.dto.TransferHistoryResponse;
import com.revana.bank.transaction.dto.TransferRequest;
import com.revana.bank.transaction.dto.TransferResponse;
import com.revana.bank.transaction.entity.TransferStatus;
import com.revana.bank.transaction.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST controller for the Fund Transfer feature.
 *
 * All endpoints require a valid JWT in the Authorization header.
 * Role enforcement is applied at the method level via @PreAuthorize.
 *
 * Base path: /api/transfers
 */
@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Fund Transfer", description = "APIs for initiating and querying fund transfers")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    // =========================================================================
    // POST /api/transfers — Initiate a fund transfer
    // =========================================================================

    /**
     * Initiates a fund transfer between two accounts.
     *
     * Accessible by: CUSTOMER only (as per requirements).
     * The authenticated user's identity is extracted from the JWT via the
     * request attribute set by JwtAuthenticationFilter.
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary     = "Initiate fund transfer",
            description = "Transfer funds from source account to destination account. " +
                          "Daily limit: ₹5,00,000 per account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or self-transfer"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "422", description = "Insufficient balance / daily limit / inactive account"),
            @ApiResponse(responseCode = "401", description = "JWT token missing or invalid")
    })
    public ResponseEntity<TransferResponse> initiateTransfer(
            @Valid @RequestBody TransferRequest request,
            HttpServletRequest httpRequest) {

        // Extract userId stamped by JwtAuthenticationFilter
        String userId = resolveUserId(httpRequest);

        TransferResponse response = transferService.initiateTransfer(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // GET /api/transfers/{transactionReference} — Lookup by reference
    // =========================================================================

    /**
     * Returns the transfer record for the given transaction reference.
     *
     * Accessible by: CUSTOMER + ADMIN.
     */
    @GetMapping("/{transactionReference}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(
            summary     = "Get transfer by reference",
            description = "Retrieve a single transfer using its unique transaction reference."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer found"),
            @ApiResponse(responseCode = "404", description = "Transfer not found"),
            @ApiResponse(responseCode = "401", description = "JWT token missing or invalid")
    })
    public ResponseEntity<TransferResponse> getByReference(
            @Parameter(description = "Unique transaction reference, e.g. TXN4A3B2C1D5E6F")
            @PathVariable String transactionReference) {

        return ResponseEntity.ok(transferService.getByReference(transactionReference));
    }

    // =========================================================================
    // GET /api/transfers/history — Paginated transfer history
    // =========================================================================

    /**
     * Returns a paginated transfer history for a source account.
     *
     * All filter parameters are optional:
     *   - status    → filter by PENDING / SUCCESS / FAILED / REVERSED
     *   - from / to → filter by date range (ISO 8601 format)
     *
     * Default pagination: 10 items per page, sorted by createdAt DESC.
     *
     * Accessible by: CUSTOMER + ADMIN.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(
            summary     = "Get transfer history",
            description = "Returns paginated transfer history for a source account. " +
                          "Optionally filter by status and/or date range."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History returned"),
            @ApiResponse(responseCode = "401", description = "JWT token missing or invalid")
    })
    public ResponseEntity<Page<TransferHistoryResponse>> getHistory(

            @Parameter(description = "Source account number, e.g. ACC10001", required = true)
            @RequestParam String sourceAccount,

            @Parameter(description = "Filter by status: PENDING, SUCCESS, FAILED, REVERSED")
            @RequestParam(required = false) TransferStatus status,

            @Parameter(description = "Start of date range (ISO 8601), e.g. 2026-01-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(description = "End of date range (ISO 8601), e.g. 2026-12-31T23:59:59")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(
                transferService.getHistory(sourceAccount, status, from, to, pageable));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves the caller's userId from the request attribute set by
     * JwtAuthenticationFilter, or falls back to the authenticated username.
     */
    private String resolveUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("X-UserId");
        if (userId != null) {
            return userId.toString();
        }
        // Fallback: use the username from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "UNKNOWN";
    }
}
