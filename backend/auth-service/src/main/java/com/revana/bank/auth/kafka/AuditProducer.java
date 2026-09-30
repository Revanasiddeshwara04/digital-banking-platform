package com.revana.bank.auth.kafka;

import com.revana.bank.auth.dto.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditProducer {

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;

    public void publish(AuditEvent event) {

        kafkaTemplate.send(
                "audit-events",
                event.getAuditId(),
                event
        );
    }
}