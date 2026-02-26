package com.samjenkins.budget_service.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record UpsertBudgetCategoryLimitRequest(
    @Positive long limitCents,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "colorHex must be in #RRGGBB format") String colorHex
) {}
