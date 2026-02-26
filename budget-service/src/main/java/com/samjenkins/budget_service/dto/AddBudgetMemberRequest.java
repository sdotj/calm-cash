package com.samjenkins.budget_service.dto;

import com.samjenkins.budget_service.entity.BudgetRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddBudgetMemberRequest(
    @NotNull UUID userId,
    @NotNull BudgetRole role
) {}
