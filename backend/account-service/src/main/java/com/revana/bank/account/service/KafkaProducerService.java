package com.revana.bank.account.service;

import com.revana.bank.account.dto.TransferEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransferEvent(
            TransferEvent event) {

        System.out.println("SENDING EVENT = " + event);

        kafkaTemplate.send(
                "bank-transfer-topic",
                event
        ).whenComplete((result, ex) -> {

            if (ex == null) {

                System.out.println(
                        "MESSAGE SENT SUCCESSFULLY");
            } else {

                System.out.println(
                        "MESSAGE FAILED");

                ex.printStackTrace();
            }
        });
    }
}