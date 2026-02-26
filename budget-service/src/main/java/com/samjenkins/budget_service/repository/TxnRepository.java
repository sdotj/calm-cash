package com.samjenkins.budget_service.repository;

import com.samjenkins.budget_service.entity.Txn;
import com.samjenkins.budget_service.repository.projection.BudgetCategorySpendProjection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TxnRepository extends JpaRepository<Txn, UUID> {
    Optional<Txn> findByIdAndBudgetId(UUID id, UUID budgetId);

    List<Txn> findAllByBudgetIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
        UUID budgetId,
        LocalDate start,
        LocalDate end,
        Pageable pageable
    );

    List<Txn> findAllByBudgetIdAndCategoryIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
        UUID budgetId,
        UUID categoryId,
        LocalDate start,
        LocalDate end,
        Pageable pageable
    );

    @Query("""
        select t.categoryId as categoryId, coalesce(sum(-t.amountCents), 0) as spentCents
        from Txn t
        where t.budgetId = :budgetId
          and t.categoryId is not null
          and t.amountCents < 0
        group by t.categoryId
        """)
    List<BudgetCategorySpendProjection> summarizeBudgetCategoryExpenses(@Param("budgetId") UUID budgetId);

    @Query("""
        select coalesce(sum(case when t.amountCents > 0 then t.amountCents else 0 end), 0)
        from Txn t
        where t.budgetId = :budgetId
        """)
    long sumIncomeByBudget(@Param("budgetId") UUID budgetId);

    @Query("""
        select coalesce(sum(case when t.amountCents < 0 then -t.amountCents else 0 end), 0)
        from Txn t
        where t.budgetId = :budgetId
        """)
    long sumExpensesByBudget(@Param("budgetId") UUID budgetId);

    @Query("""
        select coalesce(sum(-t.amountCents), 0)
        from Txn t
        where t.budgetId = :budgetId
          and t.categoryId is null
          and t.amountCents < 0
        """)
    long sumUncategorizedExpensesByBudget(@Param("budgetId") UUID budgetId);

    @Query("""
        select coalesce(sum(-t.amountCents), 0)
        from Txn t
        where t.budgetId = :budgetId
          and t.categoryId = :categoryId
          and t.amountCents < 0
        """)
    long sumCategoryExpensesByBudget(
        @Param("budgetId") UUID budgetId,
        @Param("categoryId") UUID categoryId
    );
}
