package com.revana.bank.notification.service;

import com.revana.bank.notification.dto.NotificationRequest;
import com.revana.bank.notification.entity.Notification;
import com.revana.bank.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(
            NotificationRepository repository) {

        this.repository = repository;
    }

    public Notification createNotification(
            NotificationRequest request) {

        Notification notification =
                Notification.builder()
                        .accountId(request.getAccountId())
                        .message(request.getMessage())
                        .notificationType(
                                request.getNotificationType())
                        .status("SENT")
                        .createdAt(LocalDateTime.now())
                        .build();

        return repository.save(notification);
    }

    public List<Notification> getAllNotifications() {
        return repository.findAll();
    }

    public List<Notification> getNotifications(
            Long accountId) {

        return repository.findByAccountId(
                accountId);
    }
}