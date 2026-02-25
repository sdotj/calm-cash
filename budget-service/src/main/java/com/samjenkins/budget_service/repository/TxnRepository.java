package com.samjenkins.budget_service.repository;

import com.samjenkins.budget_service.entity.Txn;
import com.samjenkins.budget_service.repository.projection.CategoryExpenseProjection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TxnRepository extends JpaRepository<Txn, UUID> {
    Optional<Txn> findByIdAndUserId(UUID id, UUID userId);

    List<Txn> findAllByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
        UUID userId,
        LocalDate start,
        LocalDate end
    );

    @Query("""
        select coalesce(sum(case when t.amountCents > 0 then t.amountCents else 0 end), 0)
        from Txn t
        where t.userId = :userId and t.transactionDate between :start and :end
        """)
    long sumIncomeByMonth(@Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
        select coalesce(sum(case when t.amountCents < 0 then -t.amountCents else 0 end), 0)
        from Txn t
        where t.userId = :userId and t.transactionDate between :start and :end
        """)
    long sumExpensesByMonth(@Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
        select coalesce(sum(-t.amountCents), 0)
        from Txn t
        where t.userId = :userId
          and t.categoryId = :categoryId
          and t.transactionDate between :start and :end
          and t.amountCents < 0
        """)
    long sumCategoryExpenses(
        @Param("userId") UUID userId,
        @Param("categoryId") UUID categoryId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    @Query("""
        select t.categoryId as categoryId, c.name as categoryName, coalesce(sum(-t.amountCents), 0) as spentCents
        from Txn t
        left join Category c on c.id = t.categoryId and c.userId = t.userId
        where t.userId = :userId
          and t.transactionDate between :start and :end
          and t.amountCents < 0
        group by t.categoryId, c.name
        order by spentCents desc
        """)
    List<CategoryExpenseProjection> summarizeCategoryExpenses(
        @Param("userId") UUID userId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );
}
