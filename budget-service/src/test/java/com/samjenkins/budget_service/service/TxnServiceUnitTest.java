package com.samjenkins.budget_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samjenkins.budget_service.dto.CreateTransactionRequest;
import com.samjenkins.budget_service.dto.UpdateTransactionRequest;
import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.entity.BudgetCategoryLimit;
import com.samjenkins.budget_service.entity.TransactionSource;
import com.samjenkins.budget_service.entity.Txn;
import com.samjenkins.budget_service.exception.BadRequestException;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.BudgetCategoryLimitRepository;
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
class TxnServiceUnitTest {

    @Mock
    private TxnRepository txnRepository;

    @Mock
    private BudgetCategoryLimitRepository budgetCategoryLimitRepository;

    @Mock
    private BudgetAccessService budgetAccessService;

    @Mock
    private AlertService alertService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TxnService txnService;

    @Test
    void createRejectsZeroAmount() {
        UUID budgetId = UUID.randomUUID();
        CreateTransactionRequest request = new CreateTransactionRequest(
            budgetId,
            null,
            "Store",
            null,
            0L,
            LocalDate.now(),
            TransactionSource.MANUAL
        );

        assertThrows(BadRequestException.class, () -> txnService.create(UUID.randomUUID(), request));
    }

    @Test
    void createRejectsCategoryNotInBudget() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Budget budget = Budget.builder().id(budgetId).startDate(LocalDate.now().minusDays(1)).endDate(LocalDate.now().plusDays(1)).build();

        when(budgetAccessService.requireWriteAccess(userId, budgetId)).thenReturn(budget);
        when(budgetCategoryLimitRepository.findByBudgetIdAndCategoryId(budgetId, categoryId)).thenReturn(Optional.empty());

        CreateTransactionRequest request = new CreateTransactionRequest(
            budgetId,
            categoryId,
            "Store",
            null,
            -100L,
            LocalDate.now(),
            TransactionSource.MANUAL
        );

        assertThrows(NotFoundException.class, () -> txnService.create(userId, request));
    }

    @Test
    void createTriggersAlertForExpenseWithCategory() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        Budget budget = Budget.builder().id(budgetId).startDate(date.minusDays(1)).endDate(date.plusDays(1)).build();
        when(budgetAccessService.requireWriteAccess(userId, budgetId)).thenReturn(budget);
        when(budgetCategoryLimitRepository.findByBudgetIdAndCategoryId(budgetId, categoryId))
            .thenReturn(Optional.of(BudgetCategoryLimit.builder().budgetId(budgetId).categoryId(categoryId).build()));

        when(txnRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Txn txn = inv.getArgument(0);
            txn.setCreatedAt(OffsetDateTime.now());
            return txn;
        });

        CreateTransactionRequest request = new CreateTransactionRequest(
            budgetId,
            categoryId,
            "Store",
            null,
            -500L,
            date,
            TransactionSource.MANUAL
        );

        txnService.create(userId, request);

        verify(alertService).evaluateBudgetThresholdsForBudget(budgetId, categoryId);
    }

    @Test
    void createContinuesWhenAlertEvaluationFails() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate date = LocalDate.now();

        Budget budget = Budget.builder().id(budgetId).startDate(date.minusDays(1)).endDate(date.plusDays(1)).build();
        when(budgetAccessService.requireWriteAccess(userId, budgetId)).thenReturn(budget);
        when(budgetCategoryLimitRepository.findByBudgetIdAndCategoryId(budgetId, categoryId))
            .thenReturn(Optional.of(BudgetCategoryLimit.builder().budgetId(budgetId).categoryId(categoryId).build()));
        when(txnRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Txn txn = inv.getArgument(0);
            txn.setCreatedAt(OffsetDateTime.now());
            return txn;
        });
        doThrow(new RuntimeException("alert failure")).when(alertService).evaluateBudgetThresholdsForBudget(budgetId, categoryId);

        CreateTransactionRequest request = new CreateTransactionRequest(
            budgetId,
            categoryId,
            "Store",
            null,
            -500L,
            date,
            TransactionSource.MANUAL
        );

        assertDoesNotThrow(() -> txnService.create(userId, request));
    }

    @Test
    void updateRejectsLegacyTransactionFromAnotherUser() {
        UUID actorId = UUID.randomUUID();
        Txn legacy = Txn.builder().id(UUID.randomUUID()).userId(UUID.randomUUID()).budgetId(null).build();

        when(txnRepository.findById(legacy.getId())).thenReturn(Optional.of(legacy));

        UpdateTransactionRequest request = new UpdateTransactionRequest(null, null, null, null, null, null, null);
        assertThrows(NotFoundException.class, () -> txnService.update(actorId, legacy.getId(), request));
    }

    @Test
    void listByBudgetRejectsOutOfRangeQueryWindow() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget budget = Budget.builder().id(budgetId).startDate(LocalDate.of(2026, 3, 1)).endDate(LocalDate.of(2026, 3, 31)).build();
        when(budgetAccessService.requireReadAccess(userId, budgetId)).thenReturn(budget);

        assertThrows(
            BadRequestException.class,
            () -> txnService.listByBudget(userId, budgetId, null, LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 15), 50)
        );
    }

    @Test
    void listByBudgetReturnsMappedResponses() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 3, 10);
        Budget budget = Budget.builder().id(budgetId).startDate(LocalDate.of(2026, 3, 1)).endDate(LocalDate.of(2026, 3, 31)).build();
        when(budgetAccessService.requireReadAccess(userId, budgetId)).thenReturn(budget);

        Txn txn = Txn.builder().id(UUID.randomUUID()).budgetId(budgetId).merchant("Store").amountCents(-100).transactionDate(date)
            .source(TransactionSource.MANUAL).build();
        when(txnRepository.findAllByBudgetIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            any(), any(), any(), any()))
            .thenReturn(List.of(txn));

        var result = txnService.listByBudget(userId, budgetId, null, null, null, 50);

        assertEquals(1, result.size());
        assertEquals("Store", result.get(0).merchant());
        verify(txnRepository, never()).findAllByBudgetIdAndCategoryIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            any(), any(), any(), any(), any());
    }
}
