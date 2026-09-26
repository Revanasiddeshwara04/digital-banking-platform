package com.revana.bank.kyc.controller;

import com.revana.bank.kyc.dto.CreateKycRequest;
import com.revana.bank.kyc.dto.KycResponse;
import com.revana.bank.kyc.service.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @PostMapping
    public ResponseEntity<KycResponse> createKyc(
            @Valid @RequestBody CreateKycRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(kycService.createKyc(request));
    }

    @GetMapping("/{kycId}")
    public ResponseEntity<KycResponse> getKyc(
            @PathVariable String kycId) {

        return ResponseEntity.ok(
                kycService.getKyc(kycId));
    }

    @PatchMapping("/{kycId}/approve")
    public ResponseEntity<KycResponse> approveKyc(
            @PathVariable String kycId,
            @RequestParam String verifiedBy) {

        return ResponseEntity.ok(
                kycService.approveKyc(
                        kycId,
                        verifiedBy));
    }

    @PatchMapping("/{kycId}/reject")
    public ResponseEntity<KycResponse> rejectKyc(
            @PathVariable String kycId,
            @RequestParam String remarks) {

        return ResponseEntity.ok(
                kycService.rejectKyc(
                        kycId,
                        remarks));
    }
}