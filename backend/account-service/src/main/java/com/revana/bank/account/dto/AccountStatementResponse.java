package com.revana.bank.account.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AccountStatementResponse {

    private String accountNumber;
    private String customerName;
    private BigDecimal currentBalance;
    private LocalDateTime closedAt;

    private List<TransactionResponse> transactions;
}