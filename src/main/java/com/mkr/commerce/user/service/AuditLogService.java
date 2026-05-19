package com.mkr.commerce.user.service;

import com.mkr.commerce.user.dto.AuditLogDto;
import com.mkr.commerce.user.entity.AuditLog;
import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.enums.AuditAction;
import com.mkr.commerce.user.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repository;

    public void log(UUID targetId, User actor, AuditAction action, String detail) {
        repository.save(AuditLog.builder()
                .targetId(targetId)
                .actorName(actor != null ? actor.getName() : "System")
                .action(action)
                .detail(detail)
                .build());
    }

    public List<AuditLogDto> getLogsForUser(UUID targetId) {
        return repository.findByTargetIdOrderByCreatedAtDesc(targetId)
                .stream()
                .map(l -> new AuditLogDto(
                        l.getId(),
                        l.getAction().name(),
                        l.getActorName(),
                        l.getDetail(),
                        l.getCreatedAt()))
                .toList();
    }
}
