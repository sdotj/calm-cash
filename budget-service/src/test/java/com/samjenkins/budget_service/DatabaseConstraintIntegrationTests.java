package com.samjenkins.budget_service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.samjenkins.budget_service.support.IntegrationTestSupport;
import java.sql.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseConstraintIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.execute("truncate table alerts, transactions, budgets, categories cascade");
    }

    @Test
    void budgetsRejectCategoryOwnedByAnotherUser() {
        UUID categoryId = UUID.randomUUID();
        UUID categoryOwner = UUID.randomUUID();
        UUID budgetOwner = UUID.randomUUID();

        jdbcTemplate.update(
            "insert into categories (id, user_id, name) values (?, ?, ?)",
            categoryId, categoryOwner, "Dining"
        );

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            "insert into budgets (id, user_id, month, category_id, limit_cents) values (?, ?, ?, ?, ?)",
            UUID.randomUUID(), budgetOwner, Date.valueOf("2026-03-01"), categoryId, 40_000L
        ));
    }

    @Test
    void transactionsRejectCategoryOwnedByAnotherUser() {
        UUID categoryId = UUID.randomUUID();
        UUID categoryOwner = UUID.randomUUID();
        UUID txOwner = UUID.randomUUID();

        jdbcTemplate.update(
            "insert into categories (id, user_id, name) values (?, ?, ?)",
            categoryId, categoryOwner, "Groceries"
        );

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            """
            insert into transactions (
                id, user_id, category_id, merchant, description, amount_cents, transaction_date, source
            ) values (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            UUID.randomUUID(), txOwner, categoryId, "Trader Joe's", "Weekly shop", -12_500L,
            Date.valueOf("2026-03-05"), "MANUAL"
        ));
    }

    @Test
    void budgetsRequireFirstDayOfMonth() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        jdbcTemplate.update(
            "insert into categories (id, user_id, name) values (?, ?, ?)",
            categoryId, userId, "Rent"
        );

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            "insert into budgets (id, user_id, month, category_id, limit_cents) values (?, ?, ?, ?, ?)",
            UUID.randomUUID(), userId, Date.valueOf("2026-03-15"), categoryId, 120_000L
        ));
    }

    @Test
    void transactionsRejectZeroAmount() {
        UUID userId = UUID.randomUUID();

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
            """
            insert into transactions (
                id, user_id, merchant, description, amount_cents, transaction_date, source
            ) values (?, ?, ?, ?, ?, ?, ?)
            """,
            UUID.randomUUID(), userId, "Payroll", "Zero amount should fail", 0L,
            Date.valueOf("2026-03-10"), "IMPORT"
        ));
    }
}
