package com.samjenkins.budget_service.dto;

import com.samjenkins.budget_service.entity.BudgetRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateBudgetInviteRequest(
    @NotNull BudgetRole role,
    @Min(1) @Max(30) Integer expiresInDays
) {}
