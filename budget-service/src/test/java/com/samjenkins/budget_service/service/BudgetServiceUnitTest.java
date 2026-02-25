package com.samjenkins.budget_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.BudgetRepository;
import com.samjenkins.budget_service.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetServiceUnitTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BudgetService budgetService;

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
    void upsertRejectsUnknownCategory() {
        when(categoryRepository.existsByIdAndUserId(categoryId, userId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> budgetService.upsert(userId, month, categoryId, 10_000));

        verify(budgetRepository, never()).saveAndFlush(any());
    }

    @Test
    void upsertUpdatesExistingBudgetAndEvaluatesAlerts() {
        Budget existing = Budget.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .categoryId(categoryId)
            .month(month.atDay(1))
            .limitCents(5_000)
            .createdAt(java.time.OffsetDateTime.now())
            .build();

        when(categoryRepository.existsByIdAndUserId(categoryId, userId)).thenReturn(true);
        when(budgetRepository.findByUserIdAndMonthAndCategoryId(userId, month.atDay(1), categoryId))
            .thenReturn(Optional.of(existing));
        when(budgetRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = budgetService.upsert(userId, month, categoryId, 12_000);

        assertEquals(12_000, response.limitCents());
        verify(entityManager).refresh(existing);
        verify(alertService).evaluateBudgetThresholds(userId, categoryId, month);
    }

    @Test
    void listByMonthReturnsMappedBudgets() {
        Budget budget = Budget.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .categoryId(categoryId)
            .month(LocalDate.of(2026, 3, 1))
            .limitCents(40_000)
            .createdAt(java.time.OffsetDateTime.now())
            .build();

        when(budgetRepository.findAllByUserIdAndMonthOrderByCreatedAtDesc(userId, month.atDay(1)))
            .thenReturn(List.of(budget));

        var result = budgetService.listByMonth(userId, month);

        assertEquals(1, result.size());
        assertEquals(40_000, result.getFirst().limitCents());
        assertEquals(categoryId, result.getFirst().categoryId());
    }
}
