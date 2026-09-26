package com.revana.bank.notification.dto;

import lombok.Data;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferEvent {

    private Long senderId;
    private Long receiverId;

    private String senderName;
    private String receiverName;

    private BigDecimal amount;
}
