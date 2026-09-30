package com.revana.bank.auditservice.service;

import com.revana.bank.auditservice.entity.AuditLog;

import java.util.List;

public interface AuditService {

    List<AuditLog> getAll();

    AuditLog getByAuditId(String auditId);

    List<AuditLog> getByEventType(String eventType);

    List<AuditLog> getByEntityId(String entityId);
}