package com.samjenkins.budget_service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class BudgetIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.execute("truncate table budget_members, budget_category_limits, budget_plans, alerts, transactions, budgets, categories cascade");
    }

    @Test
    void supportsMultipleBudgetsForSamePeriod() throws Exception {
        UUID userId = UUID.randomUUID();
        String categoryId = createCategory(userId, "Food");

        mockMvc.perform(post("/api/budgets")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("""
                    {
                      "name":"March Core",
                      "periodType":"MONTHLY",
                      "startDate":"2026-03-01",
                      "currency":"usd",
                      "categoryLimits":[
                        {"categoryId":"%s","limitCents":20000,"colorHex":"#34a853"}
                      ]
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.categoryLimits[0].colorHex").value("#34A853"));

        mockMvc.perform(post("/api/budgets")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("""
                    {
                      "name":"March Travel",
                      "periodType":"MONTHLY",
                      "startDate":"2026-03-01",
                      "categoryLimits":[
                        {"categoryId":"%s","limitCents":10000,"colorHex":"#FBBC05"}
                      ]
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/budgets")
                .header(AUTHORIZATION, bearer(userId))
                .param("periodType", "MONTHLY")
                .param("status", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsWeeklyBudgetWhenStartDateIsNotMonday() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/budgets")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("""
                    {
                      "name":"Bad Weekly",
                      "periodType":"WEEKLY",
                      "startDate":"2026-03-03"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("WEEKLY budget startDate must align with Monday"));
    }

    @Test
    void upsertsAndDeletesCategoryLimit() throws Exception {
        UUID userId = UUID.randomUUID();
        String categoryId = createCategory(userId, "Subscriptions");

        String budgetResponse = mockMvc.perform(post("/api/budgets")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("""
                    {
                      "name":"April 2026",
                      "periodType":"MONTHLY",
                      "startDate":"2026-04-01"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String budgetId = JsonPath.read(budgetResponse, "$.id");

        mockMvc.perform(put("/api/budgets/{budgetId}/categories/{categoryId}", budgetId, categoryId)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("{" +
                    "\"limitCents\":12000," +
                    "\"colorHex\":\"#4285F4\"" +
                    "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.limitCents").value(12000))
            .andExpect(jsonPath("$.colorHex").value("#4285F4"));

        mockMvc.perform(delete("/api/budgets/{budgetId}/categories/{categoryId}", budgetId, categoryId)
                .header(AUTHORIZATION, bearer(userId)))
            .andExpect(status().isNoContent());
    }

    @Test
    void archivedBudgetIsReadOnlyButCanBeUnarchived() throws Exception {
        UUID userId = UUID.randomUUID();
        String categoryId = createCategory(userId, "Dining");
        String budgetId = createBudget(userId, categoryId);

        mockMvc.perform(patch("/api/budgets/{budgetId}", budgetId)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("{\"status\":\"ARCHIVED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(post("/api/transactions")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("""
                    {
                      "budgetId":"%s",
                      "categoryId":"%s",
                      "merchant":"Cafe",
                      "amountCents":-1500,
                      "transactionDate":"2026-03-08",
                      "source":"MANUAL"
                    }
                    """.formatted(budgetId, categoryId)))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/budgets/{budgetId}/categories/{categoryId}", budgetId, categoryId)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("{\"limitCents\":18000,\"colorHex\":\"#34A853\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/budgets/{budgetId}", budgetId)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/transactions")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("""
                    {
                      "budgetId":"%s",
                      "categoryId":"%s",
                      "merchant":"Cafe",
                      "amountCents":-1500,
                      "transactionDate":"2026-03-08",
                      "source":"MANUAL"
                    }
                    """.formatted(budgetId, categoryId)))
            .andExpect(status().isCreated());
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

    private String bearer(UUID userId) {
        return "Bearer " + JwtTestTokens.valid(userId);
    }

    private String createBudget(UUID ownerId, String categoryId) throws Exception {
        String response = mockMvc.perform(post("/api/budgets")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(ownerId))
                .content("""
                    {
                      "name":"March Core",
                      "periodType":"MONTHLY",
                      "startDate":"2026-03-01",
                      "categoryLimits":[{"categoryId":"%s","limitCents":20000,"colorHex":"#34A853"}]
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
