package com.revana.bank.kyc.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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