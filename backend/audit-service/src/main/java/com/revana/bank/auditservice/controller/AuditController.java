package com.revana.bank.auditservice.controller;

import com.revana.bank.auditservice.entity.AuditLog;
import com.revana.bank.auditservice.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public List<AuditLog> getAll() {
        return auditService.getAll();
    }

    @GetMapping("/{auditId}")
    public AuditLog getByAuditId(
            @PathVariable String auditId) {

        return auditService.getByAuditId(auditId);
    }

    @GetMapping("/event/{eventType}")
    public List<AuditLog> getByEventType(
            @PathVariable String eventType) {

        return auditService.getByEventType(eventType);
    }

    @GetMapping("/entity/{entityId}")
    public List<AuditLog> getByEntityId(
            @PathVariable String entityId) {

        return auditService.getByEntityId(entityId);
    }
}