package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.dto.BudgetResponse;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.BudgetRepository;
import com.samjenkins.budget_service.repository.CategoryRepository;
import com.samjenkins.budget_service.entity.Budget;
import jakarta.persistence.EntityManager;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final AlertService alertService;
    private final EntityManager entityManager;

    @Transactional
    public BudgetResponse upsert(UUID userId, YearMonth month, UUID categoryId, long limitCents) {
        if (!categoryRepository.existsByIdAndUserId(categoryId, userId)) {
            throw new NotFoundException("Category not found");
        }

        Budget budget = budgetRepository.findByUserIdAndMonthAndCategoryId(userId, month.atDay(1), categoryId)
            .orElseGet(() -> Budget.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .month(month.atDay(1))
                .categoryId(categoryId)
                .build());

        budget.setLimitCents(limitCents);
        Budget saved = budgetRepository.saveAndFlush(budget);
        entityManager.refresh(saved);

        alertService.evaluateBudgetThresholds(userId, categoryId, month);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> listByMonth(UUID userId, YearMonth month) {
        return budgetRepository.findAllByUserIdAndMonthOrderByCreatedAtDesc(userId, month.atDay(1)).stream()
            .map(this::toResponse)
            .toList();
    }

    private BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
            budget.getId(),
            budget.getMonth(),
            budget.getCategoryId(),
            budget.getLimitCents(),
            budget.getCreatedAt()
        );
    }
}
