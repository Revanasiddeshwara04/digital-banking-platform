package com.revana.bank.notification.dto;

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