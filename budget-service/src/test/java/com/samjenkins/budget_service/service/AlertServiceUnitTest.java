package com.samjenkins.budget_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samjenkins.budget_service.entity.Alert;
import com.samjenkins.budget_service.entity.AlertType;
import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.entity.Category;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.AlertRepository;
import com.samjenkins.budget_service.repository.BudgetRepository;
import com.samjenkins.budget_service.repository.CategoryRepository;
import com.samjenkins.budget_service.repository.TxnRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AlertServiceUnitTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TxnRepository txnRepository;

    @InjectMocks
    private AlertService alertService;

    private UUID userId;
    private UUID categoryId;
    private YearMonth month;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        month = YearMonth.of(2026, 3);
    }

    @Test
    void evaluateBudgetThresholdsCreates80And100AlertsWhenExceeded() {
        Budget budget = Budget.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .categoryId(categoryId)
            .month(month.atDay(1))
            .limitCents(10_000)
            .build();

        when(budgetRepository.findByUserIdAndMonthAndCategoryId(userId, month.atDay(1), categoryId))
            .thenReturn(Optional.of(budget));
        when(txnRepository.sumCategoryExpenses(userId, categoryId, month.atDay(1), month.atEndOfMonth()))
            .thenReturn(11_000L);
        when(categoryRepository.findByIdAndUserId(categoryId, userId))
            .thenReturn(Optional.of(Category.builder().id(categoryId).userId(userId).name("Dining").build()));
        when(alertRepository.existsByUserIdAndTypeAndCategoryIdAndMonthAndThresholdPct(
            userId, AlertType.BUDGET_80, categoryId, month.atDay(1), 80)).thenReturn(false);
        when(alertRepository.existsByUserIdAndTypeAndCategoryIdAndMonthAndThresholdPct(
            userId, AlertType.BUDGET_100, categoryId, month.atDay(1), 100)).thenReturn(false);

        alertService.evaluateBudgetThresholds(userId, categoryId, month);

        verify(alertRepository, times(2)).save(any(Alert.class));
    }

    @Test
    void evaluateBudgetThresholdsSkipsWhenNoBudget() {
        when(budgetRepository.findByUserIdAndMonthAndCategoryId(userId, month.atDay(1), categoryId))
            .thenReturn(Optional.empty());

        alertService.evaluateBudgetThresholds(userId, categoryId, month);

        verify(alertRepository, never()).save(any());
    }

    @Test
    void listUsesDbLimitInsteadOfInMemoryLimit() {
        Alert alert = Alert.builder().id(UUID.randomUUID()).userId(userId).type(AlertType.SYSTEM).message("m").build();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(alertRepository.findAllByUserIdOrderByCreatedAtDesc(eq(userId), pageableCaptor.capture()))
            .thenReturn(List.of(alert));

        var result = alertService.list(userId, false, 20);

        assertEquals(1, result.size());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void markReadSetsReadAtForUnreadAlert() {
        Alert alert = Alert.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .type(AlertType.SYSTEM)
            .message("m")
            .build();
        when(alertRepository.findByIdAndUserId(alert.getId(), userId)).thenReturn(Optional.of(alert));

        var response = alertService.markRead(userId, alert.getId());

        assertNotNull(response.readAt());
    }

    @Test
    void markReadThrowsWhenAlertMissing() {
        UUID alertId = UUID.randomUUID();
        when(alertRepository.findByIdAndUserId(alertId, userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> alertService.markRead(userId, alertId));
    }
}
