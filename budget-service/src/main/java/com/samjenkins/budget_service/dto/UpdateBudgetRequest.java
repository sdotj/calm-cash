package com.samjenkins.budget_service.dto;

import com.samjenkins.budget_service.entity.BudgetStatus;
import jakarta.validation.constraints.Size;

public record UpdateBudgetRequest(
    @Size(min = 1, max = 150) String name,
    BudgetStatus status
) {}
