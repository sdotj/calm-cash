package com.samjenkins.budget_service.dto;

import java.util.UUID;

public record AcceptBudgetInviteResponse(
    UUID budgetId,
    String role,
    String status
) {}
