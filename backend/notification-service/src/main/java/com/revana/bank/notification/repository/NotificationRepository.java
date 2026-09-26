package com.revana.bank.notification.repository;

import com.revana.bank.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByAccountId(
            Long accountId);
}