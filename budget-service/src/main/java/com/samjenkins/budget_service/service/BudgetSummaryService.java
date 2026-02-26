package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.dto.BudgetSummaryCategoryResponse;
import com.samjenkins.budget_service.dto.BudgetSummaryResponse;
import com.samjenkins.budget_service.entity.Category;
import com.samjenkins.budget_service.entity.BudgetCategoryLimit;
import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.repository.CategoryRepository;
import com.samjenkins.budget_service.repository.TxnRepository;
import com.samjenkins.budget_service.repository.BudgetCategoryLimitRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetSummaryService {

    private static final String UNCATEGORIZED = "Uncategorized";

    private final BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    private final CategoryRepository categoryRepository;
    private final TxnRepository txnRepository;
    private final BudgetAccessService budgetAccessService;

    @Transactional(readOnly = true)
    public BudgetSummaryResponse summarize(UUID userId, UUID budgetId) {
        Budget budget = budgetAccessService.requireReadAccess(userId, budgetId);

        List<BudgetCategoryLimit> limits = budgetCategoryLimitRepository.findAllByBudgetIdOrderByCreatedAtAsc(budgetId);
        Map<UUID, Long> spentByCategory = txnRepository.summarizeBudgetCategoryExpenses(budgetId).stream()
            .collect(java.util.stream.Collectors.toMap(
                row -> row.getCategoryId(),
                row -> row.getSpentCents(),
                (left, right) -> right
            ));

        Map<UUID, String> categoryNames = categoryRepository.findAllByIdIn(
                limits.stream().map(BudgetCategoryLimit::getCategoryId).toList())
            .stream()
            .collect(java.util.stream.Collectors.toMap(Category::getId, Category::getName));

        List<BudgetSummaryCategoryResponse> categories = new ArrayList<>();
        long totalLimitCents = 0L;
        long categorizedSpentCents = 0L;

        for (BudgetCategoryLimit limit : limits) {
            long spent = spentByCategory.getOrDefault(limit.getCategoryId(), 0L);
            long remaining = limit.getLimitCents() - spent;
            Double utilization = limit.getLimitCents() <= 0 ? null : (spent * 100.0) / limit.getLimitCents();

            categories.add(new BudgetSummaryCategoryResponse(
                limit.getCategoryId(),
                categoryNames.getOrDefault(limit.getCategoryId(), "Category"),
                limit.getColorHex(),
                limit.getLimitCents(),
                spent,
                remaining,
                utilization
            ));

            totalLimitCents += limit.getLimitCents();
            categorizedSpentCents += spent;
        }

        long uncategorizedSpent = txnRepository.sumUncategorizedExpensesByBudget(budgetId);
        if (uncategorizedSpent > 0) {
            categories.add(new BudgetSummaryCategoryResponse(
                null,
                UNCATEGORIZED,
                null,
                null,
                uncategorizedSpent,
                null,
                null
            ));
        }

        long totalSpentCents = categorizedSpentCents + uncategorizedSpent;
        long totalRemainingCents = totalLimitCents - totalSpentCents;
        Double utilizationPct = totalLimitCents <= 0 ? null : (totalSpentCents * 100.0) / totalLimitCents;

        long incomeCents = txnRepository.sumIncomeByBudget(budgetId);
        long expenseCents = txnRepository.sumExpensesByBudget(budgetId);

        return new BudgetSummaryResponse(
            budget.getId(),
            budget.getName(),
            budget.getPeriodType(),
            budget.getStartDate(),
            budget.getEndDate(),
            totalLimitCents,
            totalSpentCents,
            totalRemainingCents,
            utilizationPct,
            incomeCents,
            expenseCents,
            incomeCents - expenseCents,
            categories
        );
    }
}
