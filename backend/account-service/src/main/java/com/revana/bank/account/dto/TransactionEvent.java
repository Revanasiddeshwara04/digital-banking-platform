package com.revana.bank.account.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    private String accountNumber;
    private String transactionType;
    private BigDecimal amount;
}