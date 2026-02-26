package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.dto.AlertResponse;
import com.samjenkins.budget_service.entity.Alert;
import com.samjenkins.budget_service.entity.AlertType;
import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.AlertRepository;
import com.samjenkins.budget_service.repository.TxnRepository;
import com.samjenkins.budget_service.repository.BudgetCategoryLimitRepository;
import com.samjenkins.budget_service.repository.BudgetMemberRepository;
import com.samjenkins.budget_service.repository.BudgetRepository;
import com.samjenkins.budget_service.repository.CategoryRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final AlertRepository alertRepository;
    private final TxnRepository txnRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetMemberRepository budgetMemberRepository;
    private final BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateBudgetThresholdsForBudget(UUID budgetId, UUID categoryId) {
        var budgetOpt = budgetRepository.findById(budgetId);
        if (budgetOpt.isEmpty()) {
            return;
        }

        var limitOpt = budgetCategoryLimitRepository.findByBudgetIdAndCategoryId(budgetId, categoryId);
        if (limitOpt.isEmpty()) {
            return;
        }

        long limitCents = limitOpt.get().getLimitCents();
        long spentCents = txnRepository.sumCategoryExpensesByBudget(budgetId, categoryId);
        if (spentCents <= 0 || limitCents <= 0) {
            return;
        }

        Budget budget = budgetOpt.get();
        double usagePct = (spentCents * 100.0) / limitCents;
        String categoryName = categoryRepository.findById(categoryId)
            .map(c -> c.getName())
            .orElse("Category");
        java.util.Set<UUID> recipients = new java.util.HashSet<>();
        recipients.add(budget.getOwnerUserId());
        budgetMemberRepository.findAllByBudgetIdOrderByCreatedAtAsc(budgetId).stream()
            .map(m -> m.getUserId())
            .forEach(recipients::add);

        if (usagePct >= 80.0) {
            for (UUID recipient : recipients) {
                createBudgetAlertIfAbsentForBudget(
                    recipient,
                    budgetId,
                    categoryId,
                    budget.getStartDate(),
                    AlertType.BUDGET_80,
                    80,
                    budgetMessageForBudget(categoryName, budget.getName(), 80)
                );
            }
        }
        if (usagePct >= 100.0) {
            for (UUID recipient : recipients) {
                createBudgetAlertIfAbsentForBudget(
                    recipient,
                    budgetId,
                    categoryId,
                    budget.getStartDate(),
                    AlertType.BUDGET_100,
                    100,
                    budgetMessageForBudget(categoryName, budget.getName(), 100)
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> list(UUID userId, boolean unreadOnly, Integer requestedLimit) {
        int limit = requestedLimit == null ? DEFAULT_LIMIT : Math.min(Math.max(requestedLimit, 1), MAX_LIMIT);
        PageRequest page = PageRequest.of(0, limit);
        List<Alert> alerts = unreadOnly
            ? alertRepository.findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, page)
            : alertRepository.findAllByUserIdOrderByCreatedAtDesc(userId, page);
        return alerts.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AlertResponse markRead(UUID userId, UUID alertId) {
        Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
            .orElseThrow(() -> new NotFoundException("Alert not found"));
        if (alert.getReadAt() == null) {
            alert.setReadAt(OffsetDateTime.now());
        }
        return toResponse(alert);
    }

    private void createBudgetAlertIfAbsentForBudget(
        UUID userId,
        UUID budgetId,
        UUID categoryId,
        LocalDate month,
        AlertType type,
        int thresholdPct,
        String message
    ) {
        if (alertRepository.existsByUserIdAndTypeAndBudgetIdAndCategoryIdAndThresholdPct(
            userId, type, budgetId, categoryId, thresholdPct)) {
            return;
        }

        alertRepository.save(Alert.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .type(type)
            .message(message)
            .budgetId(budgetId)
            .categoryId(categoryId)
            .month(month)
            .thresholdPct(thresholdPct)
            .build());
    }

    private String budgetMessageForBudget(String categoryName, String budgetName, int threshold) {
        return categoryName + " reached " + threshold + "% of your " + budgetName + " budget";
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getType(), alert.getMessage(), alert.getCreatedAt(), alert.getReadAt());
    }
}
