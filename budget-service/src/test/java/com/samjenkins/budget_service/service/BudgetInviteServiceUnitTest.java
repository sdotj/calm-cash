package com.samjenkins.budget_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samjenkins.budget_service.dto.AcceptBudgetInviteResponse;
import com.samjenkins.budget_service.dto.CreateBudgetInviteRequest;
import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.entity.BudgetInvite;
import com.samjenkins.budget_service.entity.BudgetInviteStatus;
import com.samjenkins.budget_service.entity.BudgetRole;
import com.samjenkins.budget_service.exception.BadRequestException;
import com.samjenkins.budget_service.exception.ConflictException;
import com.samjenkins.budget_service.repository.BudgetInviteRepository;
import com.samjenkins.budget_service.repository.BudgetMemberRepository;
import com.samjenkins.budget_service.repository.BudgetRepository;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetInviteServiceUnitTest {

    @Mock
    private BudgetAccessService budgetAccessService;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetInviteRepository budgetInviteRepository;

    @Mock
    private BudgetMemberRepository budgetMemberRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BudgetInviteService budgetInviteService;

    @Test
    void createRejectsOwnerRole() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();

        assertThrows(
            BadRequestException.class,
            () -> budgetInviteService.create(userId, budgetId, new CreateBudgetInviteRequest(BudgetRole.OWNER, 7))
        );
    }

    @Test
    void acceptRejectsExpiredInvite() {
        UUID userId = UUID.randomUUID();
        UUID token = UUID.randomUUID();
        BudgetInvite invite = BudgetInvite.builder()
            .id(UUID.randomUUID())
            .budgetId(UUID.randomUUID())
            .token(token)
            .role(BudgetRole.EDITOR)
            .status(BudgetInviteStatus.PENDING)
            .expiresAt(OffsetDateTime.now().minusMinutes(1))
            .build();

        when(budgetInviteRepository.findByToken(token)).thenReturn(Optional.of(invite));

        assertThrows(ConflictException.class, () -> budgetInviteService.accept(userId, token));
        assertEquals(BudgetInviteStatus.REVOKED, invite.getStatus());
        verify(budgetInviteRepository).save(invite);
    }

    @Test
    void acceptRejectsOwnerAcceptingOwnBudgetInvite() {
        UUID ownerId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID token = UUID.randomUUID();

        BudgetInvite invite = BudgetInvite.builder()
            .budgetId(budgetId)
            .token(token)
            .role(BudgetRole.EDITOR)
            .status(BudgetInviteStatus.PENDING)
            .expiresAt(OffsetDateTime.now().plusDays(1))
            .build();
        Budget budget = Budget.builder().id(budgetId).ownerUserId(ownerId).build();

        when(budgetInviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        assertThrows(BadRequestException.class, () -> budgetInviteService.accept(ownerId, token));
        verify(budgetMemberRepository, never()).save(any());
    }

    @Test
    void acceptRejectsAlreadyMember() {
        UUID ownerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID token = UUID.randomUUID();

        BudgetInvite invite = BudgetInvite.builder()
            .budgetId(budgetId)
            .token(token)
            .role(BudgetRole.EDITOR)
            .status(BudgetInviteStatus.PENDING)
            .expiresAt(OffsetDateTime.now().plusDays(1))
            .build();
        Budget budget = Budget.builder().id(budgetId).ownerUserId(ownerId).build();

        when(budgetInviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(budgetMemberRepository.existsByBudgetIdAndUserId(budgetId, userId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> budgetInviteService.accept(userId, token));
    }

    @Test
    void acceptMarksInviteAcceptedAndAddsMember() {
        UUID ownerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID token = UUID.randomUUID();

        BudgetInvite invite = BudgetInvite.builder()
            .id(UUID.randomUUID())
            .budgetId(budgetId)
            .token(token)
            .role(BudgetRole.VIEWER)
            .status(BudgetInviteStatus.PENDING)
            .expiresAt(OffsetDateTime.now().plusDays(1))
            .build();
        Budget budget = Budget.builder().id(budgetId).ownerUserId(ownerId).build();

        when(budgetInviteRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(budgetMemberRepository.existsByBudgetIdAndUserId(budgetId, userId)).thenReturn(false);

        AcceptBudgetInviteResponse response = budgetInviteService.accept(userId, token);

        assertEquals("ACCEPTED", response.status());
        assertEquals("VIEWER", response.role());
        assertEquals(BudgetInviteStatus.ACCEPTED, invite.getStatus());
        assertEquals(userId, invite.getAcceptedByUserId());
        verify(budgetMemberRepository).save(any());
        verify(budgetInviteRepository).save(invite);
    }
}
