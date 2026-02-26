package com.samjenkins.budget_service.controller;

import com.samjenkins.budget_service.config.CurrentUser;
import com.samjenkins.budget_service.dto.BudgetCategoryLimitResponse;
import com.samjenkins.budget_service.dto.BudgetResponse;
import com.samjenkins.budget_service.dto.CreateBudgetRequest;
import com.samjenkins.budget_service.dto.UpdateBudgetRequest;
import com.samjenkins.budget_service.dto.UpsertBudgetCategoryLimitRequest;
import com.samjenkins.budget_service.entity.BudgetPeriodType;
import com.samjenkins.budget_service.entity.BudgetStatus;
import com.samjenkins.budget_service.service.BudgetService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@Valid @RequestBody CreateBudgetRequest request) {
        return budgetService.create(CurrentUser.userId(), request);
    }

    @GetMapping
    public List<BudgetResponse> list(
        @RequestParam(required = false) BudgetPeriodType periodType,
        @RequestParam(required = false) BudgetStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo
    ) {
        return budgetService.list(CurrentUser.userId(), periodType, status, startDateFrom, startDateTo);
    }

    @GetMapping("/{budgetId}")
    public BudgetResponse get(@PathVariable UUID budgetId) {
        return budgetService.get(CurrentUser.userId(), budgetId);
    }

    @PatchMapping("/{budgetId}")
    public BudgetResponse update(@PathVariable UUID budgetId, @Valid @RequestBody UpdateBudgetRequest request) {
        return budgetService.update(CurrentUser.userId(), budgetId, request);
    }

    @PutMapping("/{budgetId}/categories/{categoryId}")
    public BudgetCategoryLimitResponse upsertCategoryLimit(
        @PathVariable UUID budgetId,
        @PathVariable UUID categoryId,
        @Valid @RequestBody UpsertBudgetCategoryLimitRequest request
    ) {
        return budgetService.upsertCategoryLimit(CurrentUser.userId(), budgetId, categoryId, request);
    }

    @DeleteMapping("/{budgetId}/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategoryLimit(@PathVariable UUID budgetId, @PathVariable UUID categoryId) {
        budgetService.deleteCategoryLimit(CurrentUser.userId(), budgetId, categoryId);
    }
}
