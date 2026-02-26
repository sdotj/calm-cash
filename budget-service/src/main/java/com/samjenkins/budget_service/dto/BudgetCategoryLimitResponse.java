package com.samjenkins.budget_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BudgetCategoryLimitResponse(
    UUID id,
    UUID categoryId,
    String categoryName,
    long limitCents,
    String colorHex,
    long spentCents,
    long remainingCents,
    Double utilizationPct,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
