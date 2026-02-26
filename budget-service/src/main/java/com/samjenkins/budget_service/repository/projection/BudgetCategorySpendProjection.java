package com.samjenkins.budget_service.repository.projection;

import java.util.UUID;

public interface BudgetCategorySpendProjection {
    UUID getCategoryId();

    long getSpentCents();
}
