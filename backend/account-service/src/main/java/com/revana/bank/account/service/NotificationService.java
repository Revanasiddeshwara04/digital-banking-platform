package com.revana.bank.account.service;

import com.revana.bank.account.client.NotificationClient;
import com.revana.bank.account.dto.NotificationRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationClient notificationClient;

    public NotificationService(
            NotificationClient notificationClient) {

        this.notificationClient = notificationClient;
    }

    @CircuitBreaker(
            name = "notificationService",
            fallbackMethod = "notificationFallback")
    public void sendNotification(
            NotificationRequest request) {

        notificationClient.createNotification(request);
    }

    public void notificationFallback(
            NotificationRequest request,
            Throwable ex) {

        System.out.println(
                "Notification Service Down. Notification skipped.");

        System.out.println(
                "Reason = " + ex.getClass().getName());

        System.out.println(
                "Message = " + ex.getMessage());
    }
}