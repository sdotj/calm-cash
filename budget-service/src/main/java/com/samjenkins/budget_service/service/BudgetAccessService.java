package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.entity.BudgetMember;
import com.samjenkins.budget_service.entity.BudgetRole;
import com.samjenkins.budget_service.entity.BudgetStatus;
import com.samjenkins.budget_service.exception.ForbiddenException;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.BudgetMemberRepository;
import com.samjenkins.budget_service.repository.BudgetRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BudgetAccessService {

    private final BudgetRepository budgetRepository;
    private final BudgetMemberRepository budgetMemberRepository;

    public Budget requireReadAccess(UUID actorUserId, UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new NotFoundException("Budget not found"));
        if (isOwner(actorUserId, budget) || budgetMemberRepository.existsByBudgetIdAndUserId(budgetId, actorUserId)) {
            return budget;
        }
        throw new NotFoundException("Budget not found");
    }

    public Budget requireWriteAccess(UUID actorUserId, UUID budgetId) {
        Budget budget = requireWriteMembership(actorUserId, budgetId);
        ensureNotArchived(budget);
        return budget;
    }

    public Budget requireWriteAccessAllowArchived(UUID actorUserId, UUID budgetId) {
        return requireWriteMembership(actorUserId, budgetId);
    }

    public Budget requireOwner(UUID actorUserId, UUID budgetId) {
        Budget budget = requireReadAccess(actorUserId, budgetId);
        if (!isOwner(actorUserId, budget)) {
            throw new ForbiddenException("Only the budget owner can perform this action");
        }
        ensureNotArchived(budget);
        return budget;
    }

    public boolean isOwner(UUID userId, Budget budget) {
        return budget.getOwnerUserId().equals(userId);
    }

    private Budget requireWriteMembership(UUID actorUserId, UUID budgetId) {
        Budget budget = requireReadAccess(actorUserId, budgetId);
        if (isOwner(actorUserId, budget)) {
            return budget;
        }

        BudgetMember member = budgetMemberRepository.findByBudgetIdAndUserId(budgetId, actorUserId)
            .orElseThrow(() -> new NotFoundException("Budget not found"));
        if (member.getRole() == BudgetRole.VIEWER) {
            throw new ForbiddenException("You do not have write access to this budget");
        }
        return budget;
    }

    private void ensureNotArchived(Budget budget) {
        if (budget.getStatus() == BudgetStatus.ARCHIVED) {
            throw new ForbiddenException("Archived budgets are read-only");
        }
    }
}
