package com.samjenkins.budget_service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class SharedBudgetIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.execute("truncate table budget_members, budget_category_limits, budget_plans, alerts, transactions, budgets, categories cascade");
    }

    @Test
    void editorCanReadAndWriteSharedBudgetWhileViewerIsReadOnly() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();

        String categoryId = createCategory(ownerId, "Family Groceries");
        String budgetId = createBudget(ownerId, categoryId);

        mockMvc.perform(post("/api/budgets/{budgetId}/members", budgetId)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(ownerId))
                .content("{\"userId\":\"" + editorId + "\",\"role\":\"EDITOR\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("EDITOR"));

        mockMvc.perform(post("/api/budgets/{budgetId}/members", budgetId)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(ownerId))
                .content("{\"userId\":\"" + viewerId + "\",\"role\":\"VIEWER\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("VIEWER"));

        mockMvc.perform(get("/api/budgets/{budgetId}", budgetId)
                .header(AUTHORIZATION, bearer(editorId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(budgetId));

        mockMvc.perform(post("/api/transactions")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(editorId))
                .content("""
                    {
                      "budgetId":"%s",
                      "categoryId":"%s",
                      "merchant":"Costco",
                      "amountCents":-5500,
                      "transactionDate":"2026-03-08",
                      "source":"MANUAL"
                    }
                    """.formatted(budgetId, categoryId)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(viewerId))
                .content("""
                    {
                      "budgetId":"%s",
                      "categoryId":"%s",
                      "merchant":"Target",
                      "amountCents":-2200,
                      "transactionDate":"2026-03-09",
                      "source":"MANUAL"
                    }
                    """.formatted(budgetId, categoryId)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/budgets/{budgetId}/summary", budgetId)
                .header(AUTHORIZATION, bearer(viewerId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expenseCents").value(5500));
    }

    @Test
    void nonMemberCannotAccessSharedBudget() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();

        String categoryId = createCategory(ownerId, "Trips");
        String budgetId = createBudget(ownerId, categoryId);

        mockMvc.perform(get("/api/budgets/{budgetId}", budgetId)
                .header(AUTHORIZATION, bearer(strangerId)))
            .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/transactions/{transactionId}", UUID.randomUUID())
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(strangerId))
                .content("{\"budgetId\":\"" + budgetId + "\"}"))
            .andExpect(status().isNotFound());
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

    private String createBudget(UUID ownerId, String categoryId) throws Exception {
        String response = mockMvc.perform(post("/api/budgets")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(ownerId))
                .content("""
                    {
                      "name":"Family March",
                      "periodType":"MONTHLY",
                      "startDate":"2026-03-01",
                      "categoryLimits":[{"categoryId":"%s","limitCents":30000,"colorHex":"#34A853"}]
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String bearer(UUID userId) {
        return "Bearer " + JwtTestTokens.valid(userId);
    }
}
