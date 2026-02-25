package com.samjenkins.budget_service.dto;

import java.util.UUID;

public record MonthlyCategorySummary(
    UUID categoryId,
    String categoryName,
    long spentCents,
    Long budgetLimitCents,
    Double utilizationPct
) {}
