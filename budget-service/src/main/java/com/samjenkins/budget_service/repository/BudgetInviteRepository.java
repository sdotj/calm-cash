package com.samjenkins.budget_service.repository;

import com.samjenkins.budget_service.entity.BudgetInvite;
import com.samjenkins.budget_service.entity.BudgetInviteStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetInviteRepository extends JpaRepository<BudgetInvite, UUID> {
    Optional<BudgetInvite> findByToken(UUID token);

    Optional<BudgetInvite> findByIdAndBudgetId(UUID id, UUID budgetId);

    List<BudgetInvite> findAllByBudgetIdOrderByCreatedAtDesc(UUID budgetId);

    List<BudgetInvite> findAllByBudgetIdAndStatusOrderByCreatedAtDesc(UUID budgetId, BudgetInviteStatus status);
}
