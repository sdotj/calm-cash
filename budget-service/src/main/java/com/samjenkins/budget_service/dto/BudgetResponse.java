package com.samjenkins.budget_service.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BudgetResponse(
    UUID id,
    LocalDate month,
    UUID categoryId,
    long limitCents,
    OffsetDateTime createdAt
) {}
