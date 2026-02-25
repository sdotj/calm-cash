package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.dto.MonthlyCategorySummary;
import com.samjenkins.budget_service.dto.MonthlySummaryResponse;
import com.samjenkins.budget_service.repository.BudgetRepository;
import com.samjenkins.budget_service.repository.TxnRepository;
import com.samjenkins.budget_service.util.MonthUtil;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonthlySummaryService {

    private static final String UNCATEGORIZED = "Uncategorized";

    private final TxnRepository txnRepository;
    private final BudgetRepository budgetRepository;

    @Transactional(readOnly = true)
    public MonthlySummaryResponse summarize(UUID userId, YearMonth month) {
        var start = month.atDay(1);
        var end = month.atEndOfMonth();

        long income = txnRepository.sumIncomeByMonth(userId, start, end);
        long expenses = txnRepository.sumExpensesByMonth(userId, start, end);

        Map<UUID, Long> budgetByCategory = budgetRepository.findAllByUserIdAndMonthOrderByCreatedAtDesc(userId, start)
            .stream()
            .collect(java.util.stream.Collectors.toMap(
                b -> b.getCategoryId(),
                b -> b.getLimitCents(),
                (left, right) -> right
            ));

        List<MonthlyCategorySummary> categories = txnRepository.summarizeCategoryExpenses(userId, start, end)
            .stream()
            .map(row -> {
                Long limit = row.getCategoryId() == null ? null : budgetByCategory.get(row.getCategoryId());
                Double utilization = (limit == null || limit <= 0) ? null : (row.getSpentCents() * 100.0) / limit;
                String name = row.getCategoryName() == null ? UNCATEGORIZED : row.getCategoryName();
                return new MonthlyCategorySummary(row.getCategoryId(), name, row.getSpentCents(), limit, utilization);
            })
            .toList();

        return new MonthlySummaryResponse(MonthUtil.format(month), income, expenses, income - expenses, categories);
    }
}
