package com.revana.bank.notification.kafka;

import com.revana.bank.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerActivatedConsumer {

    private final EmailService emailService;

    @KafkaListener(
            topics = "customer-activated-topic",
            groupId = "notification-group"
    )
    public void consume(
            CustomerActivatedEvent event) {

        log.info(
                "Customer Activated Event Received: {}",
                event);

        String subject =
                "Welcome to FinCore Bank";

        String body =
                "Dear "
                        + event.getFirstName()
                        + ",\n\n"
                        + "Your KYC has been approved successfully.\n"
                        + "Customer ID : "
                        + event.getCustomerId()
                        + "\n"
                        + "CIF Number : "
                        + event.getCifNumber()
                        + "\n\n"
                        + "Your account is now ACTIVE.\n\n"
                        + "Thank you,\n"
                        + "FinCore Bank";

        emailService.sendEmail(
                event.getEmail(),
                subject,
                body);
    }
}