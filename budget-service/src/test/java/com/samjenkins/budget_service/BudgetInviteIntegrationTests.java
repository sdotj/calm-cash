package com.samjenkins.budget_service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class BudgetInviteIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.execute("truncate table budget_invites, budget_members, budget_category_limits, budget_plans, alerts, transactions, budgets, categories cascade");
    }

    @Test
    void inviteAcceptFlowAddsMemberAndAllowsAccess() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();

        String categoryId = createCategory(ownerId, "Household");
        String budgetId = createBudget(ownerId, categoryId);

        String inviteResponse = mockMvc.perform(post("/api/budgets/{budgetId}/invites", budgetId)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(ownerId))
                .content("{\"role\":\"EDITOR\",\"expiresInDays\":7}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(inviteResponse, "$.token");

        mockMvc.perform(post("/api/budget-invites/{token}/accept", token)
                .header(AUTHORIZATION, bearer(inviteeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.role").value("EDITOR"));

        mockMvc.perform(get("/api/budgets/{budgetId}", budgetId)
                .header(AUTHORIZATION, bearer(inviteeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(budgetId));

        mockMvc.perform(post("/api/transactions")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(inviteeId))
                .content("""
                    {
                      "budgetId":"%s",
                      "categoryId":"%s",
                      "merchant":"Walmart",
                      "amountCents":-4200,
                      "transactionDate":"2026-03-10",
                      "source":"MANUAL"
                    }
                    """.formatted(budgetId, categoryId)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/budget-invites/{token}/accept", token)
                .header(AUTHORIZATION, bearer(UUID.randomUUID())))
            .andExpect(status().isConflict());
    }

    @Test
    void revokedInviteCannotBeAccepted() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();

        String categoryId = createCategory(ownerId, "Subscriptions");
        String budgetId = createBudget(ownerId, categoryId);

        String inviteResponse = mockMvc.perform(post("/api/budgets/{budgetId}/invites", budgetId)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(ownerId))
                .content("{\"role\":\"VIEWER\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String inviteId = JsonPath.read(inviteResponse, "$.id");
        String token = JsonPath.read(inviteResponse, "$.token");

        mockMvc.perform(delete("/api/budgets/{budgetId}/invites/{inviteId}", budgetId, inviteId)
                .header(AUTHORIZATION, bearer(ownerId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/budget-invites/{token}/accept", token)
                .header(AUTHORIZATION, bearer(inviteeId)))
            .andExpect(status().isConflict());
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
                      "name":"Shared March",
                      "periodType":"MONTHLY",
                      "startDate":"2026-03-01",
                      "categoryLimits":[{"categoryId":"%s","limitCents":25000,"colorHex":"#FBBC05"}]
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
