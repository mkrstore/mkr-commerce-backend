package com.mkr.commerce.user.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
    UUID    id,
    String  action,
    String  actorName,
    String  detail,
    Instant createdAt
) {}
