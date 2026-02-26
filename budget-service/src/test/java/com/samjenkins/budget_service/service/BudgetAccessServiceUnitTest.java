package com.samjenkins.budget_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.entity.BudgetMember;
import com.samjenkins.budget_service.entity.BudgetRole;
import com.samjenkins.budget_service.entity.BudgetStatus;
import com.samjenkins.budget_service.exception.ForbiddenException;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.BudgetMemberRepository;
import com.samjenkins.budget_service.repository.BudgetRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetAccessServiceUnitTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetMemberRepository budgetMemberRepository;

    @InjectMocks
    private BudgetAccessService budgetAccessService;

    @Test
    void requireReadAccessReturnsBudgetForOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget budget = Budget.builder().id(budgetId).ownerUserId(ownerId).build();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        Budget result = budgetAccessService.requireReadAccess(ownerId, budgetId);
        assertEquals(budgetId, result.getId());
    }

    @Test
    void requireReadAccessReturnsBudgetForMember() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget budget = Budget.builder().id(budgetId).ownerUserId(ownerId).build();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(budgetMemberRepository.existsByBudgetIdAndUserId(budgetId, memberId)).thenReturn(true);

        Budget result = budgetAccessService.requireReadAccess(memberId, budgetId);
        assertEquals(budgetId, result.getId());
    }

    @Test
    void requireReadAccessThrowsForNonMember() {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget budget = Budget.builder().id(budgetId).ownerUserId(ownerId).build();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(budgetMemberRepository.existsByBudgetIdAndUserId(budgetId, strangerId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> budgetAccessService.requireReadAccess(strangerId, budgetId));
    }

    @Test
    void requireWriteAccessThrowsForViewer() {
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget budget = Budget.builder().id(budgetId).ownerUserId(ownerId).build();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(budgetMemberRepository.existsByBudgetIdAndUserId(budgetId, viewerId)).thenReturn(true);
        when(budgetMemberRepository.findByBudgetIdAndUserId(budgetId, viewerId))
            .thenReturn(Optional.of(BudgetMember.builder().budgetId(budgetId).userId(viewerId).role(BudgetRole.VIEWER).build()));

        assertThrows(ForbiddenException.class, () -> budgetAccessService.requireWriteAccess(viewerId, budgetId));
    }

    @Test
    void requireOwnerThrowsForEditor() {
        UUID ownerId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget budget = Budget.builder().id(budgetId).ownerUserId(ownerId).build();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(budgetMemberRepository.existsByBudgetIdAndUserId(budgetId, editorId)).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> budgetAccessService.requireOwner(editorId, budgetId));
    }

    @Test
    void requireWriteAccessThrowsForArchivedBudget() {
        UUID ownerId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget budget = Budget.builder().id(budgetId).ownerUserId(ownerId).status(BudgetStatus.ARCHIVED).build();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        assertThrows(ForbiddenException.class, () -> budgetAccessService.requireWriteAccess(ownerId, budgetId));
    }
}
