package com.revana.bank.account.kafka;

import com.revana.bank.account.dto.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(AuditEvent event) {

        kafkaTemplate.send(
                "audit-events",
                event
        );

        System.out.println(
                "AUDIT EVENT SENT : "
                        + event.getEventType()
        );
    }
}