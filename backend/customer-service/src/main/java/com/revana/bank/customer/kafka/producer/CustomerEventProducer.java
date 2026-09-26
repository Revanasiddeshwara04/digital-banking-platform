package com.revana.bank.customer.kafka.producer;

import com.revana.bank.customer.kafka.event.CustomerCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CUSTOMER_CREATED_TOPIC =
            "customer-created-topic";

    public void publishCustomerCreatedEvent(
            CustomerCreatedEvent event) {

        try {

            kafkaTemplate.send(
                    CUSTOMER_CREATED_TOPIC,
                    event.getCustomerId(),
                    event
            );

            log.info(
                    "CUSTOMER CREATED EVENT PUBLISHED : {}",
                    event
            );

        } catch (Exception ex) {

            log.error(
                    "FAILED TO PUBLISH CUSTOMER EVENT : {}",
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }
    }
}