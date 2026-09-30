package com.revana.bank.auditservice.kafka;

import com.revana.bank.auditservice.dto.AuditEvent;
import com.revana.bank.auditservice.entity.AuditLog;
import com.revana.bank.auditservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditConsumer {

    private final AuditLogRepository repository;

    @KafkaListener(
            topics = "audit-events",
            groupId = "audit-service-group"
    )
    public void consume(AuditEvent event) {

        log.info("Received Audit Event {}", event);

        if (repository.findByAuditId(event.getAuditId()).isPresent()) {
            return;
        }

        AuditLog auditLog =
                AuditLog.builder()
                        .auditId(event.getAuditId())
                        .eventType(event.getEventType())
                        .serviceName(event.getServiceName())
                        .entityId(event.getEntityId())
                        .performedBy(event.getPerformedBy())
                        .actionStatus(event.getActionStatus())
                        .eventTimestamp(event.getEventTimestamp())
                        .payload(event.getPayload())
                        .build();

        repository.save(auditLog);
    }
}