package com.samjenkins.budget_service.controller;

import com.samjenkins.budget_service.config.CurrentUser;
import com.samjenkins.budget_service.dto.AddBudgetMemberRequest;
import com.samjenkins.budget_service.dto.BudgetMemberResponse;
import com.samjenkins.budget_service.service.BudgetMemberService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetMemberController {

    private final BudgetMemberService budgetMemberService;

    @PostMapping("/{budgetId}/members")
    public BudgetMemberResponse addOrUpdateMember(
        @PathVariable UUID budgetId,
        @Valid @RequestBody AddBudgetMemberRequest request
    ) {
        return budgetMemberService.addOrUpdateMember(CurrentUser.userId(), budgetId, request);
    }

    @GetMapping("/{budgetId}/members")
    public List<BudgetMemberResponse> listMembers(@PathVariable UUID budgetId) {
        return budgetMemberService.listMembers(CurrentUser.userId(), budgetId);
    }

    @DeleteMapping("/{budgetId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID budgetId, @PathVariable UUID memberUserId) {
        budgetMemberService.removeMember(CurrentUser.userId(), budgetId, memberUserId);
    }
}
