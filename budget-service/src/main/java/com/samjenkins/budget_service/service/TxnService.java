package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.dto.CreateTransactionRequest;
import com.samjenkins.budget_service.dto.TransactionResponse;
import com.samjenkins.budget_service.dto.UpdateTransactionRequest;
import com.samjenkins.budget_service.entity.Txn;
import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.exception.BadRequestException;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.BudgetCategoryLimitRepository;
import com.samjenkins.budget_service.repository.TxnRepository;
import com.samjenkins.budget_service.service.AlertService;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class TxnService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final TxnRepository txnRepository;
    private final BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    private final BudgetAccessService budgetAccessService;
    private final AlertService alertService;
    private final EntityManager entityManager;

    @Transactional
    public TransactionResponse create(UUID userId, CreateTransactionRequest request) {
        validateAmount(request.amountCents());
        Budget budget = budgetAccessService.requireWriteAccess(userId, request.budgetId());
        validateDateInBudgetRange(request.transactionDate(), budget);
        validateCategoryInBudget(budget.getId(), request.categoryId());

        Txn txn = Txn.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .budgetId(budget.getId())
            .categoryId(request.categoryId())
            .merchant(request.merchant().trim())
            .description(trimToNull(request.description()))
            .amountCents(request.amountCents())
            .transactionDate(request.transactionDate())
            .source(request.source())
            .updatedAt(OffsetDateTime.now())
            .build();

        Txn saved = txnRepository.saveAndFlush(txn);
        entityManager.refresh(saved);
        evaluateThresholdIfExpense(saved);
        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID transactionId, UpdateTransactionRequest request) {
        Txn existing = txnRepository.findById(transactionId)
            .orElseThrow(() -> new NotFoundException("Transaction not found"));
        if (existing.getBudgetId() == null && !existing.getUserId().equals(userId)) {
            throw new NotFoundException("Transaction not found");
        }
        if (existing.getBudgetId() == null && request.budgetId() == null) {
            throw new BadRequestException("budgetId is required");
        }
        Budget existingBudget = existing.getBudgetId() == null
            ? null
            : budgetAccessService.requireWriteAccess(userId, existing.getBudgetId());

        if (request.merchant() != null) {
            if (request.merchant().isBlank()) {
                throw new BadRequestException("Merchant cannot be blank");
            }
            existing.setMerchant(request.merchant().trim());
        }
        if (request.description() != null) {
            existing.setDescription(trimToNull(request.description()));
        }
        if (request.amountCents() != null) {
            validateAmount(request.amountCents());
            existing.setAmountCents(request.amountCents());
        }
        if (request.source() != null) {
            existing.setSource(request.source());
        }
        if (request.categoryId() != null) {
            UUID categoryBudgetId = existingBudget == null
                ? (request.budgetId() == null ? null : request.budgetId())
                : existingBudget.getId();
            validateCategoryInBudget(categoryBudgetId, request.categoryId());
            existing.setCategoryId(request.categoryId());
        }

        UUID targetBudgetId = request.budgetId() != null ? request.budgetId() : existing.getBudgetId();
        if (targetBudgetId == null) {
            throw new BadRequestException("budgetId is required");
        }
        Budget budget = budgetAccessService.requireWriteAccess(userId, targetBudgetId);
        existing.setBudgetId(targetBudgetId);
        if (request.categoryId() == null && existing.getCategoryId() != null) {
            validateCategoryInBudget(budget.getId(), existing.getCategoryId());
        }

        if (request.transactionDate() != null) {
            existing.setTransactionDate(request.transactionDate());
        }
        validateDateInBudgetRange(existing.getTransactionDate(), budget);

        existing.setUpdatedAt(OffsetDateTime.now());
        Txn saved = txnRepository.saveAndFlush(existing);
        entityManager.refresh(saved);
        evaluateThresholdIfExpense(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listByBudget(
        UUID userId,
        UUID budgetId,
        UUID categoryId,
        java.time.LocalDate minDate,
        java.time.LocalDate maxDate,
        Integer requestedLimit
    ) {
        Budget budget = budgetAccessService.requireReadAccess(userId, budgetId);
        if (categoryId != null) {
            validateCategoryInBudget(budgetId, categoryId);
        }

        int limit = requestedLimit == null ? DEFAULT_LIMIT : Math.min(Math.max(requestedLimit, 1), MAX_LIMIT);
        java.time.LocalDate effectiveMin = minDate == null ? budget.getStartDate() : minDate;
        java.time.LocalDate effectiveMax = maxDate == null ? budget.getEndDate() : maxDate;
        if (effectiveMin.isAfter(effectiveMax)) {
            throw new BadRequestException("minDate cannot be after maxDate");
        }
        if (effectiveMin.isBefore(budget.getStartDate()) || effectiveMax.isAfter(budget.getEndDate())) {
            throw new BadRequestException("Date range must be within budget period");
        }

        PageRequest page = PageRequest.of(0, limit);
        List<Txn> txns = categoryId == null
            ? txnRepository.findAllByBudgetIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                budgetId, effectiveMin, effectiveMax, page)
            : txnRepository.findAllByBudgetIdAndCategoryIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                budgetId, categoryId, effectiveMin, effectiveMax, page);

        return txns.stream().map(this::toResponse).toList();
    }

    private void validateCategoryInBudget(UUID budgetId, UUID categoryId) {
        if (categoryId == null) {
            return;
        }
        if (budgetId == null) {
            throw new BadRequestException("budgetId is required");
        }
        if (budgetCategoryLimitRepository.findByBudgetIdAndCategoryId(budgetId, categoryId).isEmpty()) {
            throw new NotFoundException("Category not in budget");
        }
    }

    private void validateDateInBudgetRange(java.time.LocalDate date, Budget budget) {
        if (date.isBefore(budget.getStartDate()) || date.isAfter(budget.getEndDate())) {
            throw new BadRequestException("transactionDate must fall within budget period");
        }
    }

    private void validateAmount(long amountCents) {
        if (amountCents == 0L) {
            throw new BadRequestException("amountCents must be non-zero");
        }
    }

    private void evaluateThresholdIfExpense(Txn txn) {
        if (txn.getAmountCents() < 0 && txn.getBudgetId() != null && txn.getCategoryId() != null) {
            enqueueAlertEvaluation(txn.getBudgetId(), txn.getCategoryId(), txn.getId());
        }
    }

    private void enqueueAlertEvaluation(UUID budgetId, UUID categoryId, UUID txnId) {
        Runnable task = () -> {
            try {
                alertService.evaluateBudgetThresholdsForBudget(budgetId, categoryId);
            } catch (RuntimeException ex) {
                log.warn(
                    "Alert evaluation failed for budgetId={} categoryId={} txnId={}; continuing",
                    budgetId,
                    categoryId,
                    txnId,
                    ex
                );
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TransactionResponse toResponse(Txn txn) {
        return new TransactionResponse(
            txn.getId(),
            txn.getBudgetId(),
            txn.getCategoryId(),
            txn.getMerchant(),
            txn.getDescription(),
            txn.getAmountCents(),
            txn.getTransactionDate(),
            txn.getSource(),
            txn.getCreatedAt(),
            txn.getUpdatedAt()
        );
    }
}
