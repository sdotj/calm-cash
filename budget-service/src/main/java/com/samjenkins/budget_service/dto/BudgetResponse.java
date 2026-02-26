package com.samjenkins.budget_service.dto;

import com.samjenkins.budget_service.entity.BudgetPeriodType;
import com.samjenkins.budget_service.entity.BudgetStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BudgetResponse(
    UUID id,
    String name,
    BudgetPeriodType periodType,
    LocalDate startDate,
    LocalDate endDate,
    String currency,
    BudgetStatus status,
    long totalLimitCents,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<BudgetCategoryLimitResponse> categoryLimits
) {}
