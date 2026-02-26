package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.dto.AddBudgetMemberRequest;
import com.samjenkins.budget_service.dto.BudgetMemberResponse;
import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.entity.BudgetMember;
import com.samjenkins.budget_service.entity.BudgetRole;
import com.samjenkins.budget_service.exception.BadRequestException;
import com.samjenkins.budget_service.exception.NotFoundException;
import com.samjenkins.budget_service.repository.BudgetMemberRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetMemberService {

    private final BudgetAccessService budgetAccessService;
    private final BudgetMemberRepository budgetMemberRepository;
    private final EntityManager entityManager;

    @Transactional
    public BudgetMemberResponse addOrUpdateMember(UUID userId, UUID budgetId, AddBudgetMemberRequest request) {
        Budget budget = budgetAccessService.requireOwner(userId, budgetId);
        if (request.role() == BudgetRole.OWNER) {
            throw new BadRequestException("Role OWNER cannot be assigned as a member");
        }
        if (request.userId().equals(budget.getOwnerUserId())) {
            throw new BadRequestException("Owner is implicitly a member");
        }

        BudgetMember member = budgetMemberRepository.findByBudgetIdAndUserId(budgetId, request.userId())
            .orElseGet(() -> BudgetMember.builder()
                .id(UUID.randomUUID())
                .budgetId(budgetId)
                .userId(request.userId())
                .build());
        member.setRole(request.role());

        BudgetMember saved = budgetMemberRepository.saveAndFlush(member);
        entityManager.refresh(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BudgetMemberResponse> listMembers(UUID userId, UUID budgetId) {
        Budget budget = budgetAccessService.requireReadAccess(userId, budgetId);
        List<BudgetMemberResponse> responses = new ArrayList<>();
        responses.add(new BudgetMemberResponse(budget.getOwnerUserId(), BudgetRole.OWNER.name(), budget.getCreatedAt()));
        budgetMemberRepository.findAllByBudgetIdOrderByCreatedAtAsc(budgetId).stream()
            .map(this::toResponse)
            .forEach(responses::add);
        return responses;
    }

    @Transactional
    public void removeMember(UUID userId, UUID budgetId, UUID memberUserId) {
        Budget budget = budgetAccessService.requireOwner(userId, budgetId);
        if (memberUserId.equals(budget.getOwnerUserId())) {
            throw new BadRequestException("Owner cannot be removed");
        }
        long deleted = budgetMemberRepository.deleteByBudgetIdAndUserId(budgetId, memberUserId);
        if (deleted == 0L) {
            throw new NotFoundException("Budget member not found");
        }
    }

    private BudgetMemberResponse toResponse(BudgetMember member) {
        return new BudgetMemberResponse(member.getUserId(), member.getRole().name(), member.getCreatedAt());
    }
}
