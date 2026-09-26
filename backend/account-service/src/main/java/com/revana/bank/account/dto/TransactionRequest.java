package com.revana.bank.account.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {

    private Long fromAccountId;

    private Long toAccountId;

    private String transactionType;

    private BigDecimal amount;

    private String status;
}