package com.revana.bank.notification.dto;

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