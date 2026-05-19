package com.mkr.commerce.user.repository;

import com.mkr.commerce.user.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByTargetIdOrderByCreatedAtDesc(UUID targetId);
}
