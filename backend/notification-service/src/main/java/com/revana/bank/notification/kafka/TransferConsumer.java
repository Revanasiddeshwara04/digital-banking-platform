package com.revana.bank.notification.kafka;

import com.revana.bank.notification.dto.TransferEvent;
import com.revana.bank.notification.entity.Notification;
import com.revana.bank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferConsumer {

    private final NotificationRepository repository;

    @KafkaListener(
            topics = "bank-transfer-topic",
            groupId = "notification-group"
    )
    public void consumeTransfer(
            TransferEvent event) {

        System.out.println("EVENT RECEIVED = " + event);

        Notification notification =
                Notification.builder()
                        .accountId(event.getReceiverId())
                        .message(
                                "₹" + event.getAmount()
                                        + " received from "
                                        + event.getSenderName())
                        .notificationType("EMAIL")
                        .status("SENT")
                        .createdAt(LocalDateTime.now())
                        .build();

        repository.save(notification);

        System.out.println(
                "Transfer Event Received: "
                        + event);
    }
}