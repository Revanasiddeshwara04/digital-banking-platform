package com.revana.bank.account.service;

import com.revana.bank.account.dto.TransactionEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransactionEvent event) {

        kafkaTemplate.send(
                "bank-transactions",
                event);

        System.out.println(
                "EVENT SENT: " + event);
    }
}