package com.samjenkins.budget_service.repository;

import com.samjenkins.budget_service.entity.Budget;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findAllByUserIdAndMonthOrderByCreatedAtDesc(UUID userId, LocalDate month);

    Optional<Budget> findByUserIdAndMonthAndCategoryId(UUID userId, LocalDate month, UUID categoryId);
}
