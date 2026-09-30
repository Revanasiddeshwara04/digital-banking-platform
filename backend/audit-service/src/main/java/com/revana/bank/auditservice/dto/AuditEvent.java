package com.revana.bank.auditservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime eventTimestamp;

    private String payload;
}