package com.samjenkins.budget_service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.samjenkins.budget_service.support.IntegrationTestSupport;
import com.samjenkins.budget_service.support.JwtTestTokens;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BudgetPhaseCIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.execute("truncate table budget_members, budget_category_limits, budget_plans, alerts, transactions, budgets, categories cascade");
    }

    @Test
    void summaryIncludesCategoryColorAndUncategorizedSpending() throws Exception {
        UUID userId = UUID.randomUUID();
        String categoryId = createCategory(userId, "Groceries");
        String budgetId = createBudget(userId, "March 2026", "MONTHLY", "2026-03-01", categoryId, 20000, "#4285F4");

        createTransaction(userId, budgetId, categoryId, -12000, "2026-03-05");
        createTransaction(userId, budgetId, null, -3000, "2026-03-06");
        createTransaction(userId, budgetId, categoryId, 50000, "2026-03-07");

        mockMvc.perform(get("/api/budgets/{budgetId}/summary", budgetId)
                .header(AUTHORIZATION, bearer(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalLimitCents").value(20000))
            .andExpect(jsonPath("$.totalSpentCents").value(15000))
            .andExpect(jsonPath("$.totalRemainingCents").value(5000))
            .andExpect(jsonPath("$.incomeCents").value(50000))
            .andExpect(jsonPath("$.expenseCents").value(15000))
            .andExpect(jsonPath("$.netCents").value(35000))
            .andExpect(jsonPath("$.categories[0].colorHex").value("#4285F4"))
            .andExpect(jsonPath("$.categories[1].categoryName").value("Uncategorized"));
    }

    @Test
    void budgetAlertsAreDedupedByBudgetCategoryAndThreshold() throws Exception {
        UUID userId = UUID.randomUUID();
        String categoryId = createCategory(userId, "Dining");
        String budgetId = createBudget(userId, "March Dining", "MONTHLY", "2026-03-01", categoryId, 10000, "#34A853");

        createTransaction(userId, budgetId, categoryId, -8000, "2026-03-04");
        createTransaction(userId, budgetId, categoryId, -500, "2026-03-05");
        createTransaction(userId, budgetId, categoryId, -1500, "2026-03-06");

        mockMvc.perform(get("/api/alerts")
                .header(AUTHORIZATION, bearer(userId))
                .param("unreadOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].type").value("BUDGET_100"))
            .andExpect(jsonPath("$[1].type").value("BUDGET_80"));
    }

    @Test
    void rejectsTransactionOutsideBudgetPeriod() throws Exception {
        UUID userId = UUID.randomUUID();
        String categoryId = createCategory(userId, "Utilities");
        String budgetId = createBudget(userId, "Week 1", "WEEKLY", "2026-03-02", categoryId, 10000, "#FBBC05");

        mockMvc.perform(post("/api/transactions")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("""
                    {
                      "budgetId":"%s",
                      "categoryId":"%s",
                      "merchant":"Utility Co",
                      "amountCents":-1200,
                      "transactionDate":"2026-03-12",
                      "source":"MANUAL"
                    }
                    """.formatted(budgetId, categoryId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("transactionDate must fall within budget period"));
    }

    private String createCategory(UUID userId, String name) throws Exception {
        String categoryResponse = mockMvc.perform(post("/api/categories")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("{\"name\":\"" + name + "\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        return JsonPath.read(categoryResponse, "$.id");
    }

    private String createBudget(
        UUID userId,
        String name,
        String periodType,
        String startDate,
        String categoryId,
        long limitCents,
        String colorHex
    ) throws Exception {
        String budgetResponse = mockMvc.perform(post("/api/budgets")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("""
                    {
                      "name":"%s",
                      "periodType":"%s",
                      "startDate":"%s",
                      "categoryLimits":[
                        {"categoryId":"%s","limitCents":%d,"colorHex":"%s"}
                      ]
                    }
                    """.formatted(name, periodType, startDate, categoryId, limitCents, colorHex)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        return JsonPath.read(budgetResponse, "$.id");
    }

    private void createTransaction(
        UUID userId,
        String budgetId,
        String categoryId,
        long amountCents,
        String transactionDate
    ) throws Exception {
        String categoryPart = categoryId == null ? "\"categoryId\":null," : "\"categoryId\":\"" + categoryId + "\",";
        mockMvc.perform(post("/api/transactions")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("{" +
                    "\"budgetId\":\"" + budgetId + "\"," +
                    categoryPart +
                    "\"merchant\":\"Merchant\"," +
                    "\"description\":\"desc\"," +
                    "\"amountCents\":" + amountCents + "," +
                    "\"transactionDate\":\"" + transactionDate + "\"," +
                    "\"source\":\"MANUAL\"" +
                    "}"))
            .andExpect(status().isCreated());
    }

    private String bearer(UUID userId) {
        return "Bearer " + JwtTestTokens.valid(userId);
    }
}
