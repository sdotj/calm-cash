package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.dto.AcceptBudgetInviteResponse;
import com.samjenkins.budget_service.dto.BudgetInviteResponse;
import com.samjenkins.budget_service.dto.CreateBudgetInviteRequest;
import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.entity.BudgetInvite;
import com.samjenkins.budget_service.entity.BudgetInviteStatus;
import com.samjenkins.budget_service.entity.BudgetMember;
import com.samjenkins.budget_service.entity.BudgetRole;
import com.samjenkins.budget_service.exception.BadRequestException;
import com.samjenkins.budget_service.exception.ConflictException;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.BudgetInviteRepository;
import com.samjenkins.budget_service.repository.BudgetMemberRepository;
import com.samjenkins.budget_service.repository.BudgetRepository;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetInviteService {

    private static final int DEFAULT_EXPIRY_DAYS = 7;

    private final BudgetAccessService budgetAccessService;
    private final BudgetRepository budgetRepository;
    private final BudgetInviteRepository budgetInviteRepository;
    private final BudgetMemberRepository budgetMemberRepository;
    private final EntityManager entityManager;

    @Transactional
    public BudgetInviteResponse create(UUID userId, UUID budgetId, CreateBudgetInviteRequest request) {
        budgetAccessService.requireOwner(userId, budgetId);
        if (request.role() == BudgetRole.OWNER) {
            throw new BadRequestException("Cannot invite with OWNER role");
        }

        int expiryDays = request.expiresInDays() == null ? DEFAULT_EXPIRY_DAYS : request.expiresInDays();
        BudgetInvite invite = BudgetInvite.builder()
            .id(UUID.randomUUID())
            .budgetId(budgetId)
            .invitedByUserId(userId)
            .token(UUID.randomUUID())
            .role(request.role())
            .status(BudgetInviteStatus.PENDING)
            .expiresAt(OffsetDateTime.now().plusDays(expiryDays))
            .build();

        BudgetInvite saved = budgetInviteRepository.saveAndFlush(invite);
        entityManager.refresh(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BudgetInviteResponse> list(UUID userId, UUID budgetId, BudgetInviteStatus status) {
        budgetAccessService.requireOwner(userId, budgetId);
        List<BudgetInvite> invites = status == null
            ? budgetInviteRepository.findAllByBudgetIdOrderByCreatedAtDesc(budgetId)
            : budgetInviteRepository.findAllByBudgetIdAndStatusOrderByCreatedAtDesc(budgetId, status);
        return invites.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void revoke(UUID userId, UUID budgetId, UUID inviteId) {
        budgetAccessService.requireOwner(userId, budgetId);
        BudgetInvite invite = budgetInviteRepository.findByIdAndBudgetId(inviteId, budgetId)
            .orElseThrow(() -> new NotFoundException("Invite not found"));
        if (invite.getStatus() != BudgetInviteStatus.PENDING) {
            return;
        }
        invite.setStatus(BudgetInviteStatus.REVOKED);
        budgetInviteRepository.save(invite);
    }

    @Transactional
    public AcceptBudgetInviteResponse accept(UUID userId, UUID token) {
        BudgetInvite invite = budgetInviteRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("Invite not found"));
        if (invite.getStatus() != BudgetInviteStatus.PENDING) {
            throw new ConflictException("Invite is not active");
        }
        if (invite.getExpiresAt().isBefore(OffsetDateTime.now())) {
            invite.setStatus(BudgetInviteStatus.REVOKED);
            budgetInviteRepository.save(invite);
            throw new ConflictException("Invite has expired");
        }

        Budget budget = budgetRepository.findById(invite.getBudgetId())
            .orElseThrow(() -> new NotFoundException("Budget not found"));

        if (budget.getOwnerUserId().equals(userId)) {
            throw new BadRequestException("Budget owner cannot accept invite");
        }
        if (budgetMemberRepository.existsByBudgetIdAndUserId(invite.getBudgetId(), userId)) {
            throw new ConflictException("User is already a budget member");
        }

        budgetMemberRepository.save(BudgetMember.builder()
            .id(UUID.randomUUID())
            .budgetId(invite.getBudgetId())
            .userId(userId)
            .role(invite.getRole())
            .build());

        invite.setStatus(BudgetInviteStatus.ACCEPTED);
        invite.setAcceptedByUserId(userId);
        invite.setAcceptedAt(OffsetDateTime.now());
        budgetInviteRepository.save(invite);

        return new AcceptBudgetInviteResponse(invite.getBudgetId(), invite.getRole().name(), invite.getStatus().name());
    }

    private BudgetInviteResponse toResponse(BudgetInvite invite) {
        return new BudgetInviteResponse(
            invite.getId(),
            invite.getBudgetId(),
            invite.getToken(),
            invite.getRole().name(),
            invite.getStatus().name(),
            invite.getInvitedByUserId(),
            invite.getAcceptedByUserId(),
            invite.getCreatedAt(),
            invite.getExpiresAt(),
            invite.getAcceptedAt()
        );
    }
}
