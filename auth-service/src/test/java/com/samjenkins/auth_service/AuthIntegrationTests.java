package com.samjenkins.auth_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samjenkins.auth_service.repository.RefreshTokenRepository;
import com.samjenkins.auth_service.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerReturnsTokensAndPersistsUser() throws Exception {
        String email = uniqueEmail();
        String payload = """
            {
              "email": "%s",
              "password": "super-secure-password-1",
              "displayName": "Test User"
            }
            """.formatted(email);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString());

        assertThat(userRepository.findByEmailIgnoreCase(email)).isPresent();
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        String firstPayload = """
            {
              "email": "%s",
              "password": "super-secure-password-1",
              "displayName": "Test User"
            }
            """.formatted(email);

        String duplicatePayload = """
            {
              "email": "%s",
              "password": "super-secure-password-2",
              "displayName": "Duplicate User"
            }
            """.formatted(email.toUpperCase());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstPayload))
            .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicatePayload))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    void refreshRotatesTokenAndRevokesPreviousToken() throws Exception {
        String email = uniqueEmail();
        String registerPayload = """
            {
              "email": "%s",
              "password": "super-secure-password-1",
              "displayName": "Test User"
            }
            """.formatted(email);

        String registerResponse = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String firstRefreshToken = readJson(registerResponse, "refreshToken");
        String refreshPayload = """
            {
              "refreshToken": "%s"
            }
            """.formatted(firstRefreshToken);

        String refreshResponse = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshPayload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String secondRefreshToken = readJson(refreshResponse, "refreshToken");
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshPayload))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginBlocksAfterRepeatedFailures() throws Exception {
        String email = uniqueEmail();
        String registerPayload = """
            {
              "email": "%s",
              "password": "super-secure-password-1",
              "displayName": "Test User"
            }
            """.formatted(email);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
            .andExpect(status().isOk());

        String invalidLoginPayload = """
            {
              "email": "%s",
              "password": "wrong-password-999"
            }
            """.formatted(email);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidLoginPayload))
                .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidLoginPayload))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message").value("Too many login attempts"));
    }

    private String readJson(String body, String fieldName) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(body);
        return jsonNode.get(fieldName).asText();
    }

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }
}
