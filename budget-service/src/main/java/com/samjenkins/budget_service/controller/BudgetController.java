package com.samjenkins.budget_service.controller;

import com.samjenkins.budget_service.config.CurrentUser;
import com.samjenkins.budget_service.dto.BudgetResponse;
import com.samjenkins.budget_service.dto.SetBudgetRequest;
import com.samjenkins.budget_service.service.BudgetService;
import com.samjenkins.budget_service.util.MonthUtil;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PutMapping("/{month}/{categoryId}")
    public BudgetResponse upsert(
        @PathVariable String month,
        @PathVariable UUID categoryId,
        @Valid @RequestBody SetBudgetRequest request
    ) {
        return budgetService.upsert(CurrentUser.userId(), MonthUtil.parse(month), categoryId, request.limitCents());
    }

    @GetMapping
    public List<BudgetResponse> list(@RequestParam String month) {
        return budgetService.listByMonth(CurrentUser.userId(), MonthUtil.parse(month));
    }
}
