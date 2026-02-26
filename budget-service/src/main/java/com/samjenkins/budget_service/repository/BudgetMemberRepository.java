package com.samjenkins.budget_service.repository;

import com.samjenkins.budget_service.entity.BudgetMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetMemberRepository extends JpaRepository<BudgetMember, UUID> {
    Optional<BudgetMember> findByBudgetIdAndUserId(UUID budgetId, UUID userId);

    boolean existsByBudgetIdAndUserId(UUID budgetId, UUID userId);

    List<BudgetMember> findAllByBudgetIdOrderByCreatedAtAsc(UUID budgetId);

    List<BudgetMember> findAllByUserId(UUID userId);

    long deleteByBudgetIdAndUserId(UUID budgetId, UUID userId);
}
