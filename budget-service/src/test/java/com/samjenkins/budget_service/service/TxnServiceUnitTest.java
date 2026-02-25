package com.samjenkins.budget_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samjenkins.budget_service.dto.CreateTransactionRequest;
import com.samjenkins.budget_service.dto.UpdateTransactionRequest;
import com.samjenkins.budget_service.entity.TransactionSource;
import com.samjenkins.budget_service.entity.Txn;
import com.samjenkins.budget_service.exception.BadRequestException;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.CategoryRepository;
import com.samjenkins.budget_service.repository.TxnRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
    private CategoryRepository categoryRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TxnService txnService;

    private UUID userId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
    }

    @Test
    void createRejectsZeroAmount() {
        CreateTransactionRequest request = new CreateTransactionRequest(
            categoryId, "Chipotle", "Lunch", 0L, LocalDate.of(2026, 3, 10), TransactionSource.MANUAL
        );

        assertThrows(BadRequestException.class, () -> txnService.create(userId, request));
        verify(txnRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsUnknownCategory() {
        when(categoryRepository.existsByIdAndUserId(categoryId, userId)).thenReturn(false);
        CreateTransactionRequest request = new CreateTransactionRequest(
            categoryId, "Chipotle", "Lunch", -1000L, LocalDate.of(2026, 3, 10), TransactionSource.MANUAL
        );

        assertThrows(NotFoundException.class, () -> txnService.create(userId, request));
    }

    @Test
    void createExpenseEvaluatesThresholdForMonthCategory() {
        when(categoryRepository.existsByIdAndUserId(categoryId, userId)).thenReturn(true);

        Txn saved = Txn.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .categoryId(categoryId)
            .merchant("Chipotle")
            .amountCents(-8000)
            .transactionDate(LocalDate.of(2026, 3, 10))
            .source(TransactionSource.MANUAL)
            .updatedAt(OffsetDateTime.now())
            .createdAt(OffsetDateTime.now())
            .build();
        when(txnRepository.saveAndFlush(any(Txn.class))).thenReturn(saved);

        CreateTransactionRequest request = new CreateTransactionRequest(
            categoryId, "Chipotle", "Lunch", -8000L, LocalDate.of(2026, 3, 10), TransactionSource.MANUAL
        );

        var response = txnService.create(userId, request);

        assertEquals(-8000L, response.amountCents());
        verify(entityManager).refresh(saved);
        verify(alertService).evaluateBudgetThresholds(userId, categoryId, YearMonth.of(2026, 3));
    }

    @Test
    void updateRejectsBlankMerchant() {
        UUID txnId = UUID.randomUUID();
        Txn existing = Txn.builder()
            .id(txnId)
            .userId(userId)
            .merchant("Old")
            .amountCents(-500)
            .transactionDate(LocalDate.of(2026, 3, 1))
            .source(TransactionSource.MANUAL)
            .build();
        when(txnRepository.findByIdAndUserId(txnId, userId)).thenReturn(Optional.of(existing));

        UpdateTransactionRequest request = new UpdateTransactionRequest(null, "   ", null, null, null, null);

        assertThrows(BadRequestException.class, () -> txnService.update(userId, txnId, request));
        verify(txnRepository, never()).save(any());
    }

    @Test
    void updateEvaluatesThresholdForOldAndNewExpenseCategories() {
        UUID txnId = UUID.randomUUID();
        UUID oldCategory = UUID.randomUUID();
        UUID newCategory = UUID.randomUUID();

        Txn existing = Txn.builder()
            .id(txnId)
            .userId(userId)
            .categoryId(oldCategory)
            .merchant("A")
            .amountCents(-1000)
            .transactionDate(LocalDate.of(2026, 3, 10))
            .source(TransactionSource.MANUAL)
            .updatedAt(OffsetDateTime.now())
            .build();

        when(txnRepository.findByIdAndUserId(txnId, userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByIdAndUserId(newCategory, userId)).thenReturn(true);
        when(txnRepository.save(existing)).thenReturn(existing);

        UpdateTransactionRequest request = new UpdateTransactionRequest(
            newCategory,
            "B",
            "Updated",
            -2000L,
            LocalDate.of(2026, 4, 2),
            TransactionSource.MANUAL
        );

        txnService.update(userId, txnId, request);

        verify(alertService).evaluateBudgetThresholds(userId, oldCategory, YearMonth.of(2026, 3));
        verify(alertService).evaluateBudgetThresholds(userId, newCategory, YearMonth.of(2026, 4));
        verify(alertService, times(2)).evaluateBudgetThresholds(any(), any(), any());
    }
}
