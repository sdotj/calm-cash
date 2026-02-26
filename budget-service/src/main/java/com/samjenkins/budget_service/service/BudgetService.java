package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.dto.BudgetCategoryLimitResponse;
import com.samjenkins.budget_service.dto.BudgetResponse;
import com.samjenkins.budget_service.dto.CreateBudgetCategoryLimitRequest;
import com.samjenkins.budget_service.dto.CreateBudgetRequest;
import com.samjenkins.budget_service.dto.UpdateBudgetRequest;
import com.samjenkins.budget_service.dto.UpsertBudgetCategoryLimitRequest;
import com.samjenkins.budget_service.entity.Category;
import com.samjenkins.budget_service.entity.BudgetCategoryLimit;
import com.samjenkins.budget_service.entity.BudgetPeriodType;
import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.entity.BudgetStatus;
import com.samjenkins.budget_service.exception.BadRequestException;
import com.samjenkins.budget_service.exception.ForbiddenException;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.CategoryRepository;
import com.samjenkins.budget_service.repository.TxnRepository;
import com.samjenkins.budget_service.repository.BudgetCategoryLimitRepository;
import com.samjenkins.budget_service.repository.BudgetRepository;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private static final DayOfWeek WEEK_START = DayOfWeek.MONDAY;

    private final BudgetRepository budgetRepository;
    private final BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    private final CategoryRepository categoryRepository;
    private final TxnRepository txnRepository;
    private final AlertService alertService;
    private final BudgetAccessService budgetAccessService;
    private final EntityManager entityManager;

    @Transactional
    public BudgetResponse create(UUID userId, CreateBudgetRequest request) {
        LocalDate endDate = deriveEndDate(request.periodType(), request.startDate());

        Budget budget = Budget.builder()
            .id(UUID.randomUUID())
            .ownerUserId(userId)
            .name(normalizeName(request.name()))
            .periodType(request.periodType())
            .startDate(request.startDate())
            .endDate(endDate)
            .currency(normalizeCurrency(request.currency()))
            .status(BudgetStatus.ACTIVE)
            .build();

        Budget savedBudget = budgetRepository.saveAndFlush(budget);
        entityManager.refresh(savedBudget);

        List<CreateBudgetCategoryLimitRequest> requestedLimits = request.categoryLimits() == null
            ? List.of()
            : request.categoryLimits();

        validateNoDuplicateCategories(requestedLimits);
        List<BudgetCategoryLimit> limits = requestedLimits.stream()
            .map(limit -> toCategoryLimit(savedBudget, limit))
            .toList();

        if (!limits.isEmpty()) {
            budgetCategoryLimitRepository.saveAll(limits);
        }

        return toBudgetResponse(savedBudget);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(
        UUID userId,
        BudgetPeriodType periodType,
        BudgetStatus status,
        LocalDate startDateFrom,
        LocalDate startDateTo
    ) {
        if (startDateFrom != null && startDateTo != null && startDateFrom.isAfter(startDateTo)) {
            throw new BadRequestException("startDateFrom cannot be after startDateTo");
        }

        List<Budget> base = queryPlans(userId, periodType, status);
        return base.stream()
            .filter(plan -> startDateFrom == null || !plan.getStartDate().isBefore(startDateFrom))
            .filter(plan -> startDateTo == null || !plan.getStartDate().isAfter(startDateTo))
            .map(this::toBudgetResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse get(UUID userId, UUID budgetId) {
        Budget plan = budgetAccessService.requireReadAccess(userId, budgetId);
        return toBudgetResponse(plan);
    }

    @Transactional
    public BudgetResponse update(UUID userId, UUID budgetId, UpdateBudgetRequest request) {
        if (request.name() == null && request.status() == null) {
            throw new BadRequestException("At least one field must be provided");
        }

        Budget plan = budgetAccessService.requireWriteAccessAllowArchived(userId, budgetId);
        if (plan.getStatus() == BudgetStatus.ARCHIVED) {
            boolean unarchiveOnly = request.name() == null && request.status() == BudgetStatus.ACTIVE;
            if (!unarchiveOnly) {
                throw new ForbiddenException("Archived budgets are read-only");
            }
        }

        if (request.name() != null) {
            plan.setName(normalizeName(request.name()));
        }
        if (request.status() != null) {
            plan.setStatus(request.status());
        }

        Budget saved = budgetRepository.saveAndFlush(plan);
        entityManager.refresh(saved);
        return toBudgetResponse(saved);
    }

    @Transactional
    public BudgetCategoryLimitResponse upsertCategoryLimit(
        UUID userId,
        UUID budgetId,
        UUID categoryId,
        UpsertBudgetCategoryLimitRequest request
    ) {
        Budget plan = budgetAccessService.requireWriteAccess(userId, budgetId);

        Category category = categoryRepository.findByIdAndUserId(categoryId, plan.getOwnerUserId())
            .orElseThrow(() -> new NotFoundException("Category not found"));

        BudgetCategoryLimit limit = budgetCategoryLimitRepository.findByBudgetIdAndCategoryId(budgetId, categoryId)
            .orElseGet(() -> BudgetCategoryLimit.builder()
                .id(UUID.randomUUID())
                .budgetId(budgetId)
                .userId(plan.getOwnerUserId())
                .categoryId(categoryId)
                .build());

        limit.setLimitCents(request.limitCents());
        limit.setColorHex(normalizeColorHex(request.colorHex()));

        BudgetCategoryLimit saved = budgetCategoryLimitRepository.saveAndFlush(limit);
        entityManager.refresh(saved);
        enqueueAlertEvaluation(plan.getId(), categoryId);

        long spentCents = txnRepository.summarizeBudgetCategoryExpenses(budgetId).stream()
            .filter(row -> categoryId.equals(row.getCategoryId()))
            .mapToLong(row -> row.getSpentCents())
            .findFirst()
            .orElse(0L);

        return toLimitResponse(saved, category.getName(), spentCents);
    }

    @Transactional
    public void deleteCategoryLimit(UUID userId, UUID budgetId, UUID categoryId) {
        budgetAccessService.requireWriteAccess(userId, budgetId);

        long deleted = budgetCategoryLimitRepository.deleteByBudgetIdAndCategoryId(budgetId, categoryId);
        if (deleted == 0) {
            throw new NotFoundException("Budget category limit not found");
        }
    }

    private List<Budget> queryPlans(UUID userId, BudgetPeriodType periodType, BudgetStatus status) {
        return budgetRepository.findAllAccessibleByUserIdOrderByStartDateDesc(userId).stream()
            .filter(b -> periodType == null || b.getPeriodType() == periodType)
            .filter(b -> status == null || b.getStatus() == status)
            .toList();
    }

    private BudgetResponse toBudgetResponse(Budget plan) {
        List<BudgetCategoryLimit> limits = budgetCategoryLimitRepository.findAllByBudgetIdOrderByCreatedAtAsc(plan.getId());

        Map<UUID, Long> spentByCategory = new HashMap<>();
        txnRepository.summarizeBudgetCategoryExpenses(plan.getId())
            .forEach(row -> spentByCategory.put(row.getCategoryId(), row.getSpentCents()));

        Set<UUID> categoryIds = limits.stream().map(BudgetCategoryLimit::getCategoryId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, String> categoryNames = categoryIds.isEmpty()
            ? Map.of()
            : categoryRepository.findAllByIdIn(categoryIds).stream()
                .collect(java.util.stream.Collectors.toMap(Category::getId, Category::getName));

        List<BudgetCategoryLimitResponse> limitResponses = limits.stream()
            .map(limit -> toLimitResponse(
                limit,
                categoryNames.getOrDefault(limit.getCategoryId(), "Category"),
                spentByCategory.getOrDefault(limit.getCategoryId(), 0L)
            ))
            .toList();

        long totalLimit = limitResponses.stream().mapToLong(BudgetCategoryLimitResponse::limitCents).sum();

        return new BudgetResponse(
            plan.getId(),
            plan.getName(),
            plan.getPeriodType(),
            plan.getStartDate(),
            plan.getEndDate(),
            plan.getCurrency(),
            plan.getStatus(),
            totalLimit,
            plan.getCreatedAt(),
            plan.getUpdatedAt(),
            limitResponses
        );
    }

    private BudgetCategoryLimitResponse toLimitResponse(BudgetCategoryLimit limit, String categoryName, long spentCents) {
        long remainingCents = limit.getLimitCents() - spentCents;
        Double utilization = limit.getLimitCents() <= 0 ? null : (spentCents * 100.0) / limit.getLimitCents();

        return new BudgetCategoryLimitResponse(
            limit.getId(),
            limit.getCategoryId(),
            categoryName,
            limit.getLimitCents(),
            limit.getColorHex(),
            spentCents,
            remainingCents,
            utilization,
            limit.getCreatedAt(),
            limit.getUpdatedAt()
        );
    }

    private BudgetCategoryLimit toCategoryLimit(Budget plan, CreateBudgetCategoryLimitRequest request) {
        if (!categoryRepository.existsByIdAndUserId(request.categoryId(), plan.getOwnerUserId())) {
            throw new NotFoundException("Category not found");
        }

        return BudgetCategoryLimit.builder()
            .id(UUID.randomUUID())
            .budgetId(plan.getId())
            .userId(plan.getOwnerUserId())
            .categoryId(request.categoryId())
            .limitCents(request.limitCents())
            .colorHex(normalizeColorHex(request.colorHex()))
            .build();
    }

    private void validateNoDuplicateCategories(List<CreateBudgetCategoryLimitRequest> limits) {
        Set<UUID> seen = new HashSet<>();
        for (CreateBudgetCategoryLimitRequest limit : limits) {
            if (!seen.add(limit.categoryId())) {
                throw new BadRequestException("Duplicate categoryId values are not allowed");
            }
        }
    }

    private LocalDate deriveEndDate(BudgetPeriodType periodType, LocalDate startDate) {
        if (periodType == BudgetPeriodType.MONTHLY) {
            if (startDate.getDayOfMonth() != 1) {
                throw new BadRequestException("MONTHLY budget startDate must be the first day of the month");
            }
            return startDate.withDayOfMonth(startDate.lengthOfMonth());
        }

        if (startDate.getDayOfWeek() != WEEK_START) {
            throw new BadRequestException("WEEKLY budget startDate must align with Monday");
        }
        return startDate.plusDays(6);
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new BadRequestException("name must not be blank");
        }
        return normalized;
    }

    private String normalizeCurrency(String currency) {
        String value = currency == null || currency.isBlank() ? "USD" : currency.trim().toUpperCase(Locale.ROOT);
        if (value.length() != 3 || !value.chars().allMatch(Character::isLetter)) {
            throw new BadRequestException("currency must be a 3-letter code");
        }
        return value;
    }

    private String normalizeColorHex(String colorHex) {
        if (colorHex == null || colorHex.isBlank()) {
            return null;
        }
        return colorHex.toUpperCase(Locale.ROOT);
    }

    private void enqueueAlertEvaluation(UUID budgetId, UUID categoryId) {
        Runnable task = () -> {
            try {
                alertService.evaluateBudgetThresholdsForBudget(budgetId, categoryId);
            } catch (RuntimeException ex) {
                log.warn(
                    "Alert evaluation failed for budgetId={} categoryId={} during category limit upsert; continuing",
                    budgetId,
                    categoryId,
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
}
