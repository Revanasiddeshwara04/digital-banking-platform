package com.revana.bank.account.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private String senderName;
    private String receiverName;

    private String senderAccountNumber;
    private String receiverAccountNumber;

    private String transactionType;

    private BigDecimal amount;

    private String status;

    private LocalDateTime transactionDate;
}