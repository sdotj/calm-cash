package com.samjenkins.budget_service.repository;

import com.samjenkins.budget_service.entity.BudgetCategoryLimit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetCategoryLimitRepository extends JpaRepository<BudgetCategoryLimit, UUID> {
    List<BudgetCategoryLimit> findAllByBudgetIdOrderByCreatedAtAsc(UUID budgetId);

    Optional<BudgetCategoryLimit> findByBudgetIdAndCategoryId(UUID budgetId, UUID categoryId);

    long deleteByBudgetIdAndCategoryId(UUID budgetId, UUID categoryId);
}
