package com.samjenkins.budget_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CreateBudgetCategoryLimitRequest(
    @NotNull UUID categoryId,
    @Positive long limitCents,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "colorHex must be in #RRGGBB format") String colorHex
) {}
