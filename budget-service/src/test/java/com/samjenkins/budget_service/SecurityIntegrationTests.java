package com.samjenkins.budget_service;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.samjenkins.budget_service.support.IntegrationTestSupport;
import com.samjenkins.budget_service.support.JwtTestTokens;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    void protectedRouteRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/protected-probe"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteRejectsWrongIssuer() throws Exception {
        String token = JwtTestTokens.wrongIssuer(UUID.randomUUID());
        mockMvc.perform(get("/api/protected-probe").header(AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteRejectsMalformedSubject() throws Exception {
        String token = JwtTestTokens.malformedSubject();
        mockMvc.perform(get("/api/protected-probe").header(AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteAcceptsValidToken() throws Exception {
        String token = JwtTestTokens.valid(UUID.randomUUID());
        mockMvc.perform(get("/api/protected-probe").header(AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isNotFound());
    }
}
