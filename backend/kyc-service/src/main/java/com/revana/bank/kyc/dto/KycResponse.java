package com.revana.bank.kyc.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KycResponse {

    private String kycId;

    private String customerId;

    private String cifNumber;

    private String panNumber;

    private String aadhaarNumber;

    private String status;

    private String remarks;

    private String verifiedBy;

    private LocalDateTime verifiedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}