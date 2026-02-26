package com.samjenkins.budget_service.dto;

import com.samjenkins.budget_service.entity.BudgetPeriodType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BudgetSummaryResponse(
    UUID budgetId,
    String budgetName,
    BudgetPeriodType periodType,
    LocalDate startDate,
    LocalDate endDate,
    long totalLimitCents,
    long totalSpentCents,
    long totalRemainingCents,
    Double utilizationPct,
    long incomeCents,
    long expenseCents,
    long netCents,
    List<BudgetSummaryCategoryResponse> categories
) {}
