package com.revana.bank.customer.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCustomerActivated(
            CustomerActivatedEvent event) {


        System.out.println(
                "PUBLISHING CUSTOMER ACTIVATED EVENT = "
                        + event);

        kafkaTemplate.send(
                "customer-activated-topic",
                event);
    }
}