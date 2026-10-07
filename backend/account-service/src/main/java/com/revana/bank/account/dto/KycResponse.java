package com.revana.bank.account.dto;


import lombok.Data;

@Data
public class KycResponse {

    private String kycId;

    private String customerId;

    private String cifNumber;

    private String panNumber;

    private String aadhaarNumber;

    private String status;

    private String remarks;

    private String verifiedBy;
}
