package com.samjenkins.budget_service.controller;

import com.samjenkins.budget_service.config.CurrentUser;
import com.samjenkins.budget_service.dto.BudgetSummaryResponse;
import com.samjenkins.budget_service.service.BudgetSummaryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetSummaryController {

    private final BudgetSummaryService budgetSummaryService;

    @GetMapping("/{budgetId}/summary")
    public BudgetSummaryResponse get(@PathVariable UUID budgetId) {
        return budgetSummaryService.summarize(CurrentUser.userId(), budgetId);
    }
}
