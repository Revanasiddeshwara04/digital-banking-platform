package com.revana.bank.notification.kafka;

import com.revana.bank.notification.dto.TransactionEvent;
import com.revana.bank.notification.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {

    private final EmailService emailService;

    public TransactionEventConsumer(
            EmailService emailService) {

        this.emailService = emailService;
    }

    @KafkaListener(
            topics = "bank-transactions",
            groupId = "notification-group")
    public void consume(
            TransactionEvent event) {

        System.out.println(
                "EVENT RECEIVED = " + event);

        emailService.sendEmail(
                "siddukantikar04@gmail.com",
                "Transaction Alert",
                "Transaction Type : "
                        + event.getTransactionType()
                        + "\nAmount : ₹"
                        + event.getAmount()
                        + "\nAccount : "
                        + event.getAccountNumber());
    }
}