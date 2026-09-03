package com.vennhuu.PersonalFinance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Integration test requires running PostgreSQL, Redis, and RabbitMQ infrastructure")
@SpringBootTest
class PersonalFinanceApplicationTests {

	@Test
	void contextLoads() {
	}

}

