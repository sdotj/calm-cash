package com.samjenkins.budget_service.repository;

import com.samjenkins.budget_service.entity.BudgetPeriodType;
import com.samjenkins.budget_service.entity.Budget;
import com.samjenkins.budget_service.entity.BudgetStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    Optional<Budget> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    @Query("""
        select b
        from Budget b
        where b.ownerUserId = :userId
           or exists (
                select 1
                from BudgetMember m
                where m.budgetId = b.id and m.userId = :userId
           )
        order by b.startDate desc
        """)
    List<Budget> findAllAccessibleByUserIdOrderByStartDateDesc(@Param("userId") UUID userId);

    List<Budget> findAllByOwnerUserIdOrderByStartDateDesc(UUID ownerUserId);

    List<Budget> findAllByOwnerUserIdAndStatusOrderByStartDateDesc(UUID ownerUserId, BudgetStatus status);

    List<Budget> findAllByOwnerUserIdAndPeriodTypeOrderByStartDateDesc(
        UUID ownerUserId,
        BudgetPeriodType periodType
    );

    List<Budget> findAllByOwnerUserIdAndPeriodTypeAndStatusOrderByStartDateDesc(
        UUID ownerUserId,
        BudgetPeriodType periodType,
        BudgetStatus status
    );

    boolean existsByOwnerUserIdAndPeriodTypeAndStartDateAndEndDateAndStatus(
        UUID ownerUserId,
        BudgetPeriodType periodType,
        LocalDate startDate,
        LocalDate endDate,
        BudgetStatus status
    );
}
