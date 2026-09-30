package com.revana.bank.auditservice.repository;

import com.revana.bank.auditservice.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findByAuditId(String auditId);

    List<AuditLog> findByEventType(String eventType);

    List<AuditLog> findByEntityId(String entityId);
}