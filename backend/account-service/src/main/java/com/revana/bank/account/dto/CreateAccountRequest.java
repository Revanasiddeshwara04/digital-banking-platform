package com.revana.bank.account.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {

    private Long userId;

    private String customerId;

    private String customerName;

    private String accountType;

    private BigDecimal balance;
}