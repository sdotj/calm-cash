package com.samjenkins.budget_service.dto;

import jakarta.validation.constraints.Positive;

public record SetBudgetRequest(
    @Positive long limitCents
) {}
