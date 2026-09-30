package com.revana.bank.auditservice.service;

import com.revana.bank.auditservice.entity.AuditLog;
import com.revana.bank.auditservice.exception.ResourceNotFoundException;
import com.revana.bank.auditservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository repository;

    @Override
    public List<AuditLog> getAll() {
        return repository.findAll();
    }

    @Override
    public AuditLog getByAuditId(String auditId) {

        return repository.findByAuditId(auditId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Audit not found : " + auditId));
    }

    @Override
    public List<AuditLog> getByEventType(String eventType) {
        return repository.findByEventType(eventType);
    }

    @Override
    public List<AuditLog> getByEntityId(String entityId) {
        return repository.findByEntityId(entityId);
    }
}