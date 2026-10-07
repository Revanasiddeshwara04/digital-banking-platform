package com.revana.bank.account.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    private String auditId;
    private String eventType;
    private String serviceName;
    private String entityId;
    private String performedBy;
    private String actionStatus;
    private LocalDateTime eventTimestamp;
    private String payload;
}