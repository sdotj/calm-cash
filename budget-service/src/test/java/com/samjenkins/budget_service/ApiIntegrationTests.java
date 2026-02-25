package com.samjenkins.budget_service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ApiIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.execute("truncate table alerts, transactions, budgets, categories cascade");
    }

    @Test
    void categoriesAreTenantScoped() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        jdbcTemplate.update(
            "insert into categories (id, user_id, name) values (?, ?, ?)",
            UUID.randomUUID(), userB, "Other User Category"
        );

        mockMvc.perform(get("/api/categories").header(AUTHORIZATION, bearer(userA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void budgetAndExpenseCrossingThresholdGeneratesAlert() throws Exception {
        UUID userId = UUID.randomUUID();

        String categoryResponse = mockMvc.perform(
                post("/api/categories")
                    .contentType(APPLICATION_JSON)
                    .header(AUTHORIZATION, bearer(userId))
                    .content("{\"name\":\"Dining\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String categoryId = JsonPath.read(categoryResponse, "$.id");

        mockMvc.perform(put("/api/budgets/2026-03/{categoryId}", categoryId)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("{\"limitCents\":10000}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/transactions")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, bearer(userId))
                .content("{\"categoryId\":\"" + categoryId + "\",\"merchant\":\"Chipotle\",\"description\":\"Lunch\",\"amountCents\":-8000,\"transactionDate\":\"2026-03-10\",\"source\":\"MANUAL\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/alerts")
                .header(AUTHORIZATION, bearer(userId))
                .param("unreadOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("BUDGET_80"));

        mockMvc.perform(get("/api/monthly-summary")
                .header(AUTHORIZATION, bearer(userId))
                .param("month", "2026-03"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incomeCents").value(0))
            .andExpect(jsonPath("$.expenseCents").value(8000))
            .andExpect(jsonPath("$.netCents").value(-8000));
    }

    private String bearer(UUID userId) {
        return "Bearer " + JwtTestTokens.valid(userId);
    }
}
