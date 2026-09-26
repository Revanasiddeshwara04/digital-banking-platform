package com.revana.bank.kyc.dto;

import lombok.Data;

@Data
public class CreateKycRequest {

    private String customerId;

    private String cifNumber;

    private String panNumber;

    private String aadhaarNumber;
}