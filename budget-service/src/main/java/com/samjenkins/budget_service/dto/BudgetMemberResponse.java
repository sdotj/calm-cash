package com.samjenkins.budget_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BudgetMemberResponse(
    UUID userId,
    String role,
    OffsetDateTime addedAt
) {}
