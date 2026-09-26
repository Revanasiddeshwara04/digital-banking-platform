package com.revana.bank.account.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    private Long accountId;
    private String message;
    private String notificationType;
}