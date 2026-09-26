package com.revana.bank.account.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransferResponse {

    private String senderName;
    private String receiverName;

    private String senderAccountNumber;
    private String receiverAccountNumber;

    private BigDecimal transferAmount;

    private BigDecimal senderBalance;
    private BigDecimal receiverBalance;

    private String message;
}