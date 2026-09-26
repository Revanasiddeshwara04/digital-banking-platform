package com.revana.bank.account.service;

import com.revana.bank.account.dto.NotificationEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(NotificationEvent event) {

        kafkaTemplate.send(
                "bank-notifications",
                event);

        System.out.println(
                "NOTIFICATION EVENT SENT : " + event);
    }
}