package com.revana.bank.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferEvent {

    private Long senderId;
    private Long receiverId;

    private String senderName;
    private String receiverName;

    private BigDecimal amount;
}