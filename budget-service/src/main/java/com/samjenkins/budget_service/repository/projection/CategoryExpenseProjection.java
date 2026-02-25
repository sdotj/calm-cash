package com.samjenkins.budget_service.repository.projection;

import java.util.UUID;

public interface CategoryExpenseProjection {
    UUID getCategoryId();

    String getCategoryName();

    long getSpentCents();
}
