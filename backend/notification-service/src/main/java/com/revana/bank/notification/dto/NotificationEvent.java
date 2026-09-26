package com.revana.bank.notification.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

    private Long accountId;

    private String message;

    private String notificationType;
}