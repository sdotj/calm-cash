package com.samjenkins.budget_service.dto;

import com.samjenkins.budget_service.entity.AlertType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertResponse(
    UUID id,
    AlertType type,
    String message,
    OffsetDateTime createdAt,
    OffsetDateTime readAt
) {}
