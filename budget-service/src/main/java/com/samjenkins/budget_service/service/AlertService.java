package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.dto.AlertResponse;
import com.samjenkins.budget_service.entity.Alert;
import com.samjenkins.budget_service.entity.AlertType;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.AlertRepository;
import com.samjenkins.budget_service.repository.BudgetRepository;
import com.samjenkins.budget_service.repository.CategoryRepository;
import com.samjenkins.budget_service.repository.TxnRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final AlertRepository alertRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TxnRepository txnRepository;

    @Transactional
    public void evaluateBudgetThresholds(UUID userId, UUID categoryId, YearMonth month) {
        var budgetOpt = budgetRepository.findByUserIdAndMonthAndCategoryId(userId, month.atDay(1), categoryId);
        if (budgetOpt.isEmpty()) {
            return;
        }

        long limitCents = budgetOpt.get().getLimitCents();
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        long spentCents = txnRepository.sumCategoryExpenses(userId, categoryId, start, end);

        if (spentCents <= 0 || limitCents <= 0) {
            return;
        }

        double usagePct = (spentCents * 100.0) / limitCents;
        String categoryName = categoryRepository.findByIdAndUserId(categoryId, userId)
            .map(c -> c.getName())
            .orElse("Category");
        LocalDate monthDate = month.atDay(1);

        if (usagePct >= 80.0) {
            createBudgetAlertIfAbsent(userId, categoryId, monthDate, AlertType.BUDGET_80, 80, budgetMessage(categoryName, month, 80));
        }
        if (usagePct >= 100.0) {
            createBudgetAlertIfAbsent(userId, categoryId, monthDate, AlertType.BUDGET_100, 100, budgetMessage(categoryName, month, 100));
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

    private void createBudgetAlertIfAbsent(
        UUID userId,
        UUID categoryId,
        LocalDate month,
        AlertType type,
        int thresholdPct,
        String message
    ) {
        if (alertRepository.existsByUserIdAndTypeAndCategoryIdAndMonthAndThresholdPct(
            userId, type, categoryId, month, thresholdPct)) {
            return;
        }
        alertRepository.save(Alert.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .type(type)
            .message(message)
            .categoryId(categoryId)
            .month(month)
            .thresholdPct(thresholdPct)
            .build());
    }

    private String budgetMessage(String categoryName, YearMonth month, int threshold) {
        String monthLabel = month.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + month.getYear();
        return categoryName + " reached " + threshold + "% of your " + monthLabel + " budget";
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getType(), alert.getMessage(), alert.getCreatedAt(), alert.getReadAt());
    }
}
