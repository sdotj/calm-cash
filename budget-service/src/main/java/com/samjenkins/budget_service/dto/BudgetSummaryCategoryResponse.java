package com.samjenkins.budget_service.dto;

import java.util.UUID;

public record BudgetSummaryCategoryResponse(
    UUID categoryId,
    String categoryName,
    String colorHex,
    Long limitCents,
    long spentCents,
    Long remainingCents,
    Double utilizationPct
) {}
