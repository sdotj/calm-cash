package com.samjenkins.budget_service.controller;

import com.samjenkins.budget_service.config.CurrentUser;
import com.samjenkins.budget_service.dto.AcceptBudgetInviteResponse;
import com.samjenkins.budget_service.dto.BudgetInviteResponse;
import com.samjenkins.budget_service.dto.CreateBudgetInviteRequest;
import com.samjenkins.budget_service.entity.BudgetInviteStatus;
import com.samjenkins.budget_service.service.BudgetInviteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BudgetInviteController {

    private final BudgetInviteService budgetInviteService;

    @PostMapping("/budgets/{budgetId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetInviteResponse create(
        @PathVariable UUID budgetId,
        @Valid @RequestBody CreateBudgetInviteRequest request
    ) {
        return budgetInviteService.create(CurrentUser.userId(), budgetId, request);
    }

    @GetMapping("/budgets/{budgetId}/invites")
    public List<BudgetInviteResponse> list(
        @PathVariable UUID budgetId,
        @RequestParam(required = false) BudgetInviteStatus status
    ) {
        return budgetInviteService.list(CurrentUser.userId(), budgetId, status);
    }

    @DeleteMapping("/budgets/{budgetId}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID budgetId, @PathVariable UUID inviteId) {
        budgetInviteService.revoke(CurrentUser.userId(), budgetId, inviteId);
    }

    @PostMapping("/budget-invites/{token}/accept")
    public AcceptBudgetInviteResponse accept(@PathVariable UUID token) {
        return budgetInviteService.accept(CurrentUser.userId(), token);
    }
}
