package com.samjenkins.budget_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CategoryResponse(
    UUID id,
    String name,
    OffsetDateTime createdAt
) {}
