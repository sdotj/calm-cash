package com.samjenkins.budget_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BudgetInviteResponse(
    UUID id,
    UUID budgetId,
    UUID token,
    String role,
    String status,
    UUID invitedByUserId,
    UUID acceptedByUserId,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt,
    OffsetDateTime acceptedAt
) {}
