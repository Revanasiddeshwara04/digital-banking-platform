package com.revana.bank.notification.controller;

import com.revana.bank.notification.dto.EmailRequest;
import com.revana.bank.notification.dto.NotificationRequest;
import com.revana.bank.notification.entity.Notification;
import com.revana.bank.notification.service.EmailService;
import com.revana.bank.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;
    private final EmailService emailService;

    public NotificationController(
            NotificationService service, EmailService emailService) {

        this.service = service;
        this.emailService = emailService;
    }

    @PostMapping
    public Notification createNotification(
            @RequestBody NotificationRequest request) {

        return service.createNotification(request);
    }

    @GetMapping
    public List<Notification> getAll() {

        return service.getAllNotifications();
    }

    @GetMapping("/{accountId}")
    public List<Notification> getByAccountId(
            @PathVariable Long accountId) {

        return service.getNotifications(accountId);
    }

    @PostMapping("/send-email")
    public String sendEmail(
            @RequestBody EmailRequest request) {

        emailService.sendEmail(
                request.getTo(),
                request.getSubject(),
                request.getBody());

        return "Email Sent Successfully";
    }
}