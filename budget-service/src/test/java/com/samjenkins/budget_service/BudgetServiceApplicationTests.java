package com.samjenkins.budget_service;

import com.samjenkins.budget_service.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BudgetServiceApplicationTests extends IntegrationTestSupport {

	@Test
	void contextLoads() {
	}

}
