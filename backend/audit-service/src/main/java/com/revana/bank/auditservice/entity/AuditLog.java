package com.revana.bank.auditservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String auditId;

    private String eventType;

    private String serviceName;

    private String entityId;

    private String performedBy;

    private String actionStatus;

    private LocalDateTime eventTimestamp;

    @Column(columnDefinition = "LONGTEXT")
    private String payload;
}

