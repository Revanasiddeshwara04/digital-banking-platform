package com.revana.bank.transaction.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {

    private Long fromAccountId;

    private Long toAccountId;

    private String transactionType;

    private BigDecimal amount;

    private String status;
}