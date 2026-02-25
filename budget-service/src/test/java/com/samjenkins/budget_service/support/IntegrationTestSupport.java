package com.samjenkins.budget_service.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class IntegrationTestSupport {

    public static final String TEST_ISSUER = "budgeting-auth";
    public static final String TEST_SECRET = "test-jwt-secret-for-context-loads";

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("budget_test")
        .withUsername("budget")
        .withPassword("budget");
    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("app.jwt.issuer", () -> TEST_ISSUER);
        registry.add("app.jwt.secret", () -> TEST_SECRET);
    }
}
