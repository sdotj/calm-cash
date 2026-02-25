package com.samjenkins.budget_service.dto;

import java.util.List;

public record MonthlySummaryResponse(
    String month,
    long incomeCents,
    long expenseCents,
    long netCents,
    List<MonthlyCategorySummary> categories
) {}
