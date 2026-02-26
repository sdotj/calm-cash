package com.samjenkins.budget_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.samjenkins.budget_service.dto.UpsertBudgetCategoryLimitRequest;
import com.samjenkins.budget_service.dto.BudgetResponse;
import com.samjenkins.budget_service.dto.CreateBudgetRequest;
import com.samjenkins.budget_service.dto.UpdateBudgetRequest;
import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.entity.BudgetCategoryLimit;
import com.samjenkins.budget_service.entity.Category;
import com.samjenkins.budget_service.entity.BudgetPeriodType;
import com.samjenkins.budget_service.entity.BudgetStatus;
import com.samjenkins.budget_service.exception.BadRequestException;
import com.samjenkins.budget_service.exception.ForbiddenException;
import com.samjenkins.budget_service.repository.BudgetCategoryLimitRepository;
import com.samjenkins.budget_service.repository.BudgetRepository;
import com.samjenkins.budget_service.repository.CategoryRepository;
import com.samjenkins.budget_service.repository.TxnRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    private BudgetCategoryLimitRepository budgetCategoryLimitRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TxnRepository txnRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private BudgetAccessService budgetAccessService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void createRejectsWeeklyBudgetWithNonMondayStart() {
        CreateBudgetRequest request = new CreateBudgetRequest(
            "Weekly",
            BudgetPeriodType.WEEKLY,
            LocalDate.of(2026, 3, 3),
            "USD",
            List.of()
        );

        assertThrows(BadRequestException.class, () -> budgetService.create(UUID.randomUUID(), request));
    }

    @Test
    void createNormalizesCurrencyAndComputesMonthlyEndDate() {
        UUID userId = UUID.randomUUID();
        CreateBudgetRequest request = new CreateBudgetRequest(
            "March",
            BudgetPeriodType.MONTHLY,
            LocalDate.of(2026, 3, 1),
            "usd",
            List.of()
        );

        when(budgetRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Budget budget = inv.getArgument(0);
            budget.setCreatedAt(OffsetDateTime.now());
            return budget;
        });
        when(txnRepository.summarizeBudgetCategoryExpenses(any())).thenReturn(List.of());

        BudgetResponse response = budgetService.create(userId, request);

        assertEquals("USD", response.currency());
        assertEquals(LocalDate.of(2026, 3, 31), response.endDate());
    }

    @Test
    void listRejectsInvalidDateRange() {
        assertThrows(
            BadRequestException.class,
            () -> budgetService.list(
                UUID.randomUUID(),
                null,
                null,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 3, 1)
            )
        );
    }

    @Test
    void updateRejectsEmptyPatch() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        assertThrows(BadRequestException.class, () -> budgetService.update(userId, budgetId, new UpdateBudgetRequest(null, null)));
    }

    @Test
    void updateChangesNameAndStatus() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget budget = Budget.builder()
            .id(budgetId)
            .ownerUserId(userId)
            .name("Before")
            .periodType(BudgetPeriodType.MONTHLY)
            .startDate(LocalDate.of(2026, 3, 1))
            .endDate(LocalDate.of(2026, 3, 31))
            .currency("USD")
            .status(BudgetStatus.ACTIVE)
            .build();

        when(budgetAccessService.requireWriteAccessAllowArchived(userId, budgetId)).thenReturn(budget);
        when(budgetRepository.saveAndFlush(budget)).thenReturn(budget);
        when(txnRepository.summarizeBudgetCategoryExpenses(budgetId)).thenReturn(List.of());

        var response = budgetService.update(userId, budgetId, new UpdateBudgetRequest("After", BudgetStatus.ARCHIVED));

        assertEquals("After", response.name());
        assertEquals(BudgetStatus.ARCHIVED, response.status());
    }

    @Test
    void updateRejectsNameChangeWhenBudgetIsArchived() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget archived = Budget.builder()
            .id(budgetId)
            .ownerUserId(userId)
            .status(BudgetStatus.ARCHIVED)
            .build();

        when(budgetAccessService.requireWriteAccessAllowArchived(userId, budgetId)).thenReturn(archived);

        assertThrows(
            ForbiddenException.class,
            () -> budgetService.update(userId, budgetId, new UpdateBudgetRequest("Renamed", null))
        );
    }

    @Test
    void upsertCategoryLimitContinuesWhenAlertEvaluationFails() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Budget budget = Budget.builder()
            .id(budgetId)
            .ownerUserId(userId)
            .status(BudgetStatus.ACTIVE)
            .build();
        Category category = Category.builder().id(categoryId).userId(userId).name("Food").build();
        BudgetCategoryLimit savedLimit = BudgetCategoryLimit.builder()
            .id(UUID.randomUUID())
            .budgetId(budgetId)
            .userId(userId)
            .categoryId(categoryId)
            .limitCents(12000)
            .colorHex("#34A853")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        when(budgetAccessService.requireWriteAccess(userId, budgetId)).thenReturn(budget);
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(category));
        when(budgetCategoryLimitRepository.findByBudgetIdAndCategoryId(budgetId, categoryId)).thenReturn(Optional.empty());
        when(budgetCategoryLimitRepository.saveAndFlush(any())).thenReturn(savedLimit);
        when(txnRepository.summarizeBudgetCategoryExpenses(budgetId)).thenReturn(List.of());
        doThrow(new RuntimeException("alert down")).when(alertService).evaluateBudgetThresholdsForBudget(budgetId, categoryId);

        var response = budgetService.upsertCategoryLimit(
            userId,
            budgetId,
            categoryId,
            new UpsertBudgetCategoryLimitRequest(12000, "#34A853")
        );

        assertNotNull(response);
        assertEquals(12000, response.limitCents());
    }
}
